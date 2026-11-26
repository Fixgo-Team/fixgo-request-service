package co.fixgo.request.services.impl;

import co.fixgo.request.clients.CatalogClient;
import co.fixgo.request.clients.NotificationClient;
import co.fixgo.request.clients.ServiceTypeInfo;
import co.fixgo.request.dto.CancelRequestDTO;
import co.fixgo.request.dto.CreateRequestDTO;
import co.fixgo.request.dto.RequestResponseDTO;
import co.fixgo.request.dto.UpdateRequestDTO;
import co.fixgo.request.exceptions.BusinessRuleException;
import co.fixgo.request.exceptions.ForbiddenOperationException;
import co.fixgo.request.exceptions.InvalidRequestDataException;
import co.fixgo.request.exceptions.ResourceNotFoundException;
import co.fixgo.request.mappers.RequestMapper;
import co.fixgo.request.models.RequestMode;
import co.fixgo.request.models.RequestStatus;
import co.fixgo.request.models.ServiceRequest;
import co.fixgo.request.repositories.ServiceRequestRepository;
import co.fixgo.request.security.ActorRole;
import co.fixgo.request.security.CurrentActor;
import co.fixgo.request.services.RequestService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de las solicitudes. Las dependencias llegan por constructor (RNF-30),
 * y el catálogo y las notificaciones se usan a través de interfaces (principio D de SOLID).
 */
@Service
public class RequestServiceImpl implements RequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final RequestMapper requestMapper;
    private final CatalogClient catalogClient;
    private final NotificationClient notificationClient;
    private final Clock clock;

    public RequestServiceImpl(ServiceRequestRepository serviceRequestRepository,
                              RequestMapper requestMapper,
                              CatalogClient catalogClient,
                              NotificationClient notificationClient,
                              Clock clock) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.requestMapper = requestMapper;
        this.catalogClient = catalogClient;
        this.notificationClient = notificationClient;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RequestResponseDTO create(CreateRequestDTO dto, CurrentActor actor) {
        requireRole(actor, ActorRole.CLIENT);
        ServiceTypeInfo serviceType = catalogClient.findServiceType(dto.getServiceTypeId())
                .orElseThrow(() -> new InvalidRequestDataException("serviceTypeId", "El subservicio no existe."));

        if (!serviceType.active()) {
            throw new BusinessRuleException("SERVICE_TYPE_INACTIVE", "El subservicio no está disponible en este momento.");
        }
        // RN-03: solo se pide en modo urgente un subservicio marcado como urgente
        if (dto.getMode() == RequestMode.URGENT && !serviceType.urgentAllowed()) {
            throw new BusinessRuleException("URGENT_MODE_NOT_ALLOWED", "Este subservicio no admite el modo urgente.");
        }
        validateLocation(dto.getMode(), dto.getAddress(), dto.getLatitude(), dto.getLongitude(), dto.getZoneId());

        ServiceRequest request = requestMapper.toEntity(dto);
        request.setClientId(actor.userId());
        // Anexo A.4: una urgencia empieza en PENDING y una programada en OPEN
        request.setStatus(dto.getMode() == RequestMode.URGENT ? RequestStatus.PENDING : RequestStatus.OPEN);
        // RN-09: en una urgencia el valor acordado es el precio de referencia vigente al crear
        if (dto.getMode() == RequestMode.URGENT) {
            request.setAgreedValue(serviceType.referencePrice());
        }

        ServiceRequest saved = serviceRequestRepository.save(request);
        notificationClient.notifyRequestCreated(saved);
        return requestMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestResponseDTO> findAll(Long clientId, RequestStatus status, CurrentActor actor) {
        List<ServiceRequest> requests = switch (actor.role()) {
            case CLIENT -> {
                if (clientId != null && !clientId.equals(actor.userId())) {
                    throw new ForbiddenOperationException("Solo puedes consultar tus propias solicitudes.");
                }
                yield status == null
                        ? serviceRequestRepository.findByClientIdOrderByCreatedAtDesc(actor.userId())
                        : serviceRequestRepository.findByClientIdAndStatusOrderByCreatedAtDesc(actor.userId(), status);
            }
            case PROVIDER -> {
                if (clientId != null) {
                    throw new ForbiddenOperationException("Solo puedes consultar las solicitudes que tienes asignadas.");
                }
                yield status == null
                        ? serviceRequestRepository.findByProviderIdOrderByCreatedAtDesc(actor.userId())
                        : serviceRequestRepository.findByProviderIdAndStatusOrderByCreatedAtDesc(actor.userId(), status);
            }
            case ADMIN -> findAllForAdmin(clientId, status);
            default -> throw new ForbiddenOperationException("No tienes permiso para consultar solicitudes.");
        };
        return requests.stream().map(requestMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RequestResponseDTO findById(Long id, CurrentActor actor) {
        ServiceRequest request = getOrThrow(id);
        if (!isParticipant(request, actor)) {
            throw new ForbiddenOperationException("No tienes permiso para consultar esta solicitud.");
        }
        return requestMapper.toResponse(request);
    }

    @Override
    @Transactional
    public RequestResponseDTO update(Long id, UpdateRequestDTO dto, CurrentActor actor) {
        ServiceRequest request = getOrThrow(id);
        requireOwner(request, actor);

        // RF3.3: solo antes de que sea aceptada
        if (!request.getStatus().isEditable()) {
            throw new BusinessRuleException("REQUEST_NOT_EDITABLE",
                    "La solicitud ya no se puede modificar porque está en estado " + request.getStatus() + ".");
        }
        validateLocation(request.getMode(), dto.getAddress(), dto.getLatitude(), dto.getLongitude(), dto.getZoneId());

        requestMapper.applyUpdate(request, dto);
        return requestMapper.toResponse(serviceRequestRepository.save(request));
    }

    @Override
    @Transactional
    public RequestResponseDTO cancel(Long id, CancelRequestDTO dto, CurrentActor actor) {
        ServiceRequest request = getOrThrow(id);
        if (!isParticipant(request, actor)) {
            throw new ForbiddenOperationException("No tienes permiso para cancelar esta solicitud.");
        }

        // RF3.4: solo antes de que el servicio esté en curso
        if (!request.getStatus().isCancellable()) {
            throw new BusinessRuleException("REQUEST_NOT_CANCELLABLE",
                    "La solicitud ya no se puede cancelar porque está en estado " + request.getStatus() + ".");
        }

        request.setStatus(RequestStatus.CANCELLED);
        request.setCancellationReason(dto.getReason().trim());
        request.setCancelledAt(Instant.now(clock));

        ServiceRequest saved = serviceRequestRepository.save(request);
        notificationClient.notifyStatusChanged(saved);
        return requestMapper.toResponse(saved);
    }

    private List<ServiceRequest> findAllForAdmin(Long clientId, RequestStatus status) {
        if (clientId != null && status != null) {
            return serviceRequestRepository.findByClientIdAndStatusOrderByCreatedAtDesc(clientId, status);
        }
        if (clientId != null) {
            return serviceRequestRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        }
        if (status != null) {
            return serviceRequestRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return serviceRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    private void requireRole(CurrentActor actor, ActorRole role) {
        if (!actor.is(role)) {
            throw new ForbiddenOperationException("Tu rol no tiene permiso para realizar esta operación.");
        }
    }

    /** El cliente dueño de la solicitud; ni el administrador ni el profesional editan a nombre del cliente. */
    private void requireOwner(ServiceRequest request, CurrentActor actor) {
        requireRole(actor, ActorRole.CLIENT);
        if (!request.getClientId().equals(actor.userId())) {
            throw new ForbiddenOperationException("La solicitud no te pertenece.");
        }
    }

    /** Dueño, profesional asignado o administrador. */
    private boolean isParticipant(ServiceRequest request, CurrentActor actor) {
        return switch (actor.role()) {
            case ADMIN -> true;
            case CLIENT -> request.getClientId().equals(actor.userId());
            case PROVIDER -> actor.userId().equals(request.getProviderId());
            default -> false;
        };
    }

    private ServiceRequest getOrThrow(Long id) {
        return serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La solicitud " + id + " no existe."));
    }

    /**
     * RF3.1: una urgencia necesita ubicación puntual (latitud y longitud).
     * RF3.2: una programada necesita dirección y zona.
     * En cualquier modo, latitud y longitud van juntas.
     */
    private void validateLocation(RequestMode mode, String address, BigDecimal latitude, BigDecimal longitude,
                                  Long zoneId) {
        if ((latitude == null) != (longitude == null)) {
            throw new InvalidRequestDataException(latitude == null ? "latitude" : "longitude",
                    "La latitud y la longitud deben enviarse juntas.");
        }
        if (mode == RequestMode.URGENT && latitude == null) {
            throw new InvalidRequestDataException("latitude",
                    "Una solicitud urgente necesita la ubicación (latitud y longitud).");
        }
        if (mode == RequestMode.SCHEDULED && (address == null || address.isBlank())) {
            throw new InvalidRequestDataException("address", "Una solicitud programada necesita la dirección.");
        }
        if (mode == RequestMode.SCHEDULED && zoneId == null) {
            throw new InvalidRequestDataException("zoneId", "Una solicitud programada necesita la zona.");
        }
    }
}
