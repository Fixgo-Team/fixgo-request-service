package co.fixgo.request.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de las reglas de negocio, con dobles de las interfaces (sin base de datos ni Spring).
 */
class RequestServiceImplTest {

    private final UUID serviceTypeId = UUID.randomUUID();

    private ServiceRequestRepository repository;
    private CatalogClient catalogClient;
    private NotificationClient notificationClient;
    private RequestServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ServiceRequestRepository.class);
        catalogClient = mock(CatalogClient.class);
        notificationClient = mock(NotificationClient.class);
        service = new RequestServiceImpl(repository, new RequestMapper(), catalogClient, notificationClient);

        when(catalogClient.findServiceType(serviceTypeId))
                .thenReturn(Optional.of(new ServiceTypeInfo(serviceTypeId, true, true)));
        when(repository.save(any(ServiceRequest.class))).thenAnswer(invocation -> {
            ServiceRequest saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });
    }

    @Test
    void urgentRequestStartsPendingAndNotifies() {
        RequestResponseDTO result = service.create(urgentDto());

        assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(result.getVehiclePlate()).isEqualTo("ABC123");
        verify(notificationClient).notifyRequestCreated(any(ServiceRequest.class));
    }

    @Test
    void scheduledRequestStartsOpen() {
        RequestResponseDTO result = service.create(scheduledDto());

        assertThat(result.getStatus()).isEqualTo(RequestStatus.OPEN);
    }

    @Test
    void urgentRequestWithoutLocationIsRejected() {
        CreateRequestDTO dto = urgentDto();
        dto.setLatitude(null);
        dto.setLongitude(null);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(InvalidRequestDataException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void scheduledRequestWithoutAddressIsRejected() {
        CreateRequestDTO dto = scheduledDto();
        dto.setAddress("  ");

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(InvalidRequestDataException.class);
    }

    @Test
    void urgentModeIsRejectedWhenServiceTypeDoesNotAllowIt() {
        when(catalogClient.findServiceType(serviceTypeId))
                .thenReturn(Optional.of(new ServiceTypeInfo(serviceTypeId, true, false)));

        assertThatThrownBy(() -> service.create(urgentDto()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("URGENT_MODE_NOT_ALLOWED");
    }

    @Test
    void acceptedRequestCannotBeUpdated() {
        ServiceRequest existing = existing(RequestStatus.ACCEPTED);
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setDescription("Nueva descripción");
        dto.setLatitude(new BigDecimal("2.927300"));
        dto.setLongitude(new BigDecimal("-75.281900"));

        assertThatThrownBy(() -> service.update(existing.getId(), dto))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("REQUEST_NOT_EDITABLE");
    }

    @Test
    void openRequestIsCancelledWithReason() {
        ServiceRequest existing = existing(RequestStatus.OPEN);

        RequestResponseDTO result = service.cancel(existing.getId(), new CancelRequestDTO("Ya lo solucioné"));

        assertThat(result.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        assertThat(result.getCancellationReason()).isEqualTo("Ya lo solucioné");
        assertThat(result.getCancelledAt()).isNotNull();
    }

    @Test
    void inProgressRequestCannotBeCancelled() {
        ServiceRequest existing = existing(RequestStatus.IN_PROGRESS);

        assertThatThrownBy(() -> service.cancel(existing.getId(), new CancelRequestDTO("Cambio de planes")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("REQUEST_NOT_CANCELLABLE");
    }

    @Test
    void unknownRequestReturnsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private CreateRequestDTO urgentDto() {
        CreateRequestDTO dto = new CreateRequestDTO();
        dto.setClientId(UUID.randomUUID());
        dto.setServiceTypeId(serviceTypeId);
        dto.setMode(RequestMode.URGENT);
        dto.setDescription("El carro no enciende");
        dto.setLatitude(new BigDecimal("2.927300"));
        dto.setLongitude(new BigDecimal("-75.281900"));
        dto.setVehiclePlate("abc123");
        return dto;
    }

    private CreateRequestDTO scheduledDto() {
        CreateRequestDTO dto = new CreateRequestDTO();
        dto.setClientId(UUID.randomUUID());
        dto.setServiceTypeId(serviceTypeId);
        dto.setMode(RequestMode.SCHEDULED);
        dto.setDescription("Revisar la nevera, no enfría");
        dto.setAddress("Calle 10 # 5-20, Neiva");
        return dto;
    }

    private ServiceRequest existing(RequestStatus status) {
        ServiceRequest request = new ServiceRequest();
        request.setId(UUID.randomUUID());
        request.setClientId(UUID.randomUUID());
        request.setServiceTypeId(serviceTypeId);
        request.setMode(RequestMode.URGENT);
        request.setStatus(status);
        request.setDescription("Solicitud existente");
        request.setLatitude(new BigDecimal("2.927300"));
        request.setLongitude(new BigDecimal("-75.281900"));
        when(repository.findById(request.getId())).thenReturn(Optional.of(request));
        return request;
    }
}
