package co.fixgo.request.services.impl;

import co.fixgo.request.clients.CatalogClient;
import co.fixgo.request.clients.NotificationClient;
import co.fixgo.request.clients.ServiceTypeInfo;
import co.fixgo.request.dto.CancelRequestDTO;
import co.fixgo.request.dto.CreateRequestDTO;
import co.fixgo.request.dto.RequestResponseDTO;
import co.fixgo.request.dto.UpdateRequestDTO;
import co.fixgo.request.exceptions.BusinessRuleException;
import co.fixgo.request.exceptions.InvalidRequestDataException;
import co.fixgo.request.exceptions.ResourceNotFoundException;
import co.fixgo.request.mappers.RequestMapper;
import co.fixgo.request.models.RequestMode;
import co.fixgo.request.models.RequestStatus;
import co.fixgo.request.models.ServiceRequest;
import co.fixgo.request.repositories.ServiceRequestRepository;
import co.fixgo.request.services.RequestService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de las solicitudes. Las dependencias llegan por constructor (RNF-25),
 * y el catálogo y las notificaciones se usan a través de interfaces (principio D de SOLID).
 */
@Service
public class RequestServiceImpl implements RequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final RequestMapper requestMapper;
    private final CatalogClient catalogClient;
    private final NotificationClient notificationClient;

    public RequestServiceImpl(ServiceRequestRepository serviceRequestRepository,
                              RequestMapper requestMapper,
                              CatalogClient catalogClient,
                              NotificationClient notificationClient) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.requestMapper = requestMapper;
        this.catalogClient = catalogClient;
        this.notificationClient = notificationClient;
    }

    @Override
    @Transactional
    public RequestResponseDTO create(CreateRequestDTO dto) {
        ServiceTypeInfo serviceType = catalogClient.findServiceType(dto.getServiceTypeId())
                .orElseThrow(() -> new InvalidRequestDataException("serviceTypeId", "El subservicio no existe."));

        if (!serviceType.active()) {
            throw new BusinessRuleException("SERVICE_TYPE_INACTIVE", "El subservicio no está disponible en este momento.");
        }
        // RN-03: solo se pide en modo urgente un subservicio marcado como urgente
        if (dto.getMode() == RequestMode.URGENT && !serviceType.urgentAllowed()) {
            throw new BusinessRuleException("URGENT_MODE_NOT_ALLOWED", "Este subservicio no admite el modo urgente.");
        }
        validateLocation(dto.getMode(), dto.getAddress(), dto.getLatitude(), dto.getLongitude());

        ServiceRequest request = requestMapper.toEntity(dto);
        // Anexo A.3: una urgencia empieza en PENDING y una programada en OPEN
        request.setStatus(dto.getMode() == RequestMode.URGENT ? RequestStatus.PENDING : RequestStatus.OPEN);

        ServiceRequest saved = serviceRequestRepository.save(request);
        notificationClient.notifyRequestCreated(saved);
        return requestMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestResponseDTO> findAll(UUID clientId, RequestStatus status) {
        List<ServiceRequest> requests;
        if (clientId != null && status != null) {
            requests = serviceRequestRepository.findByClientIdAndStatusOrderByCreatedAtDesc(clientId, status);
        } else if (clientId != null) {
            requests = serviceRequestRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        } else if (status != null) {
            requests = serviceRequestRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            requests = serviceRequestRepository.findAllByOrderByCreatedAtDesc();
        }
        return requests.stream().map(requestMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RequestResponseDTO findById(UUID id) {
        return requestMapper.toResponse(getOrThrow(id));
    }

    @Override
    @Transactional
    public RequestResponseDTO update(UUID id, UpdateRequestDTO dto) {
        ServiceRequest request = getOrThrow(id);

        // RF3.3: solo antes de que sea aceptada
        if (!request.getStatus().isEditable()) {
            throw new BusinessRuleException("REQUEST_NOT_EDITABLE",
                    "La solicitud ya no se puede modificar porque está en estado " + request.getStatus() + ".");
        }
        validateLocation(request.getMode(), dto.getAddress(), dto.getLatitude(), dto.getLongitude());

        requestMapper.applyUpdate(request, dto);
        return requestMapper.toResponse(serviceRequestRepository.save(request));
    }

    @Override
    @Transactional
    public RequestResponseDTO cancel(UUID id, CancelRequestDTO dto) {
        ServiceRequest request = getOrThrow(id);

        // RF3.4: solo antes de que el servicio esté en curso
        if (!request.getStatus().isCancellable()) {
            throw new BusinessRuleException("REQUEST_NOT_CANCELLABLE",
                    "La solicitud ya no se puede cancelar porque está en estado " + request.getStatus() + ".");
        }

        request.setStatus(RequestStatus.CANCELLED);
        request.setCancellationReason(dto.getReason().trim());
        request.setCancelledAt(LocalDateTime.now(ZoneOffset.UTC));

        ServiceRequest saved = serviceRequestRepository.save(request);
        notificationClient.notifyStatusChanged(saved);
        return requestMapper.toResponse(saved);
    }

    private ServiceRequest getOrThrow(UUID id) {
        return serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La solicitud " + id + " no existe."));
    }

    /**
     * RF3.1: una urgencia necesita ubicación puntual (latitud y longitud).
     * RF3.2: una programada necesita dirección.
     * En cualquier modo, latitud y longitud van juntas.
     */
    private void validateLocation(RequestMode mode, String address, BigDecimal latitude, BigDecimal longitude) {
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
    }
}
