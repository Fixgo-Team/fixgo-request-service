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
import co.fixgo.request.exceptions.ForbiddenOperationException;
import co.fixgo.request.exceptions.InvalidRequestDataException;
import co.fixgo.request.exceptions.ResourceNotFoundException;
import co.fixgo.request.mappers.RequestMapper;
import co.fixgo.request.models.RequestMode;
import co.fixgo.request.models.RequestStatus;
import co.fixgo.request.models.ServiceRequest;
import co.fixgo.request.policies.RequestTransitionPolicy;
import co.fixgo.request.policies.VehicleDataPolicy;
import co.fixgo.request.repositories.ServiceRequestRepository;
import co.fixgo.request.security.ActorRole;
import co.fixgo.request.security.CurrentActor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

/**
 * Pruebas unitarias de las reglas de negocio, con dobles de las interfaces (sin base de datos ni Spring).
 */
class RequestServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-12T01:30:00Z");

    private static final CurrentActor CLIENT = new CurrentActor(55L, ActorRole.CLIENT);
    private static final CurrentActor OTHER_CLIENT = new CurrentActor(56L, ActorRole.CLIENT);
    private static final CurrentActor PROVIDER = new CurrentActor(77L, ActorRole.PROVIDER);
    private static final CurrentActor ADMIN = new CurrentActor(1L, ActorRole.ADMIN);
    private static final CurrentActor SERVICE = new CurrentActor(9L, ActorRole.SERVICE);

    private final Long serviceTypeId = 7L;
    private final AtomicLong ids = new AtomicLong(100);

    private ServiceRequestRepository repository;
    private CatalogClient catalogClient;
    private NotificationClient notificationClient;
    private RequestServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ServiceRequestRepository.class);
        catalogClient = mock(CatalogClient.class);
        notificationClient = mock(NotificationClient.class);
        service = new RequestServiceImpl(repository, new RequestMapper(), catalogClient, notificationClient,
                new RequestTransitionPolicy(), List.of(new VehicleDataPolicy()), Clock.fixed(NOW, ZoneOffset.UTC));

        when(catalogClient.findServiceType(serviceTypeId)).thenReturn(Optional.of(serviceType(true, true)));
        when(repository.save(any(ServiceRequest.class))).thenAnswer(invocation -> {
            ServiceRequest saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(ids.incrementAndGet());
            }
            return saved;
        });
    }

    @Test
    void urgentRequestStartsPendingAndNotifies() {
        RequestResponseDTO result = service.create(urgentDto(), CLIENT);

        assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(result.getVehiclePlate()).isEqualTo("ABC123");
        verify(notificationClient).notifyRequestCreated(any(ServiceRequest.class));
    }

    @Test
    void urgentRequestStoresReferencePriceAsAgreedValue() {
        RequestResponseDTO result = service.create(urgentDto(), CLIENT);

        assertThat(result.getAgreedValue()).isEqualByComparingTo("85000.00");
    }

    @Test
    void scheduledRequestStartsOpenWithoutAgreedValue() {
        RequestResponseDTO result = service.create(scheduledDto(), CLIENT);

        assertThat(result.getStatus()).isEqualTo(RequestStatus.OPEN);
        assertThat(result.getZoneId()).isEqualTo(3L);
        assertThat(result.getAgreedValue()).isNull();
    }

    @Test
    void urgentRequestWithoutLocationIsRejected() {
        CreateRequestDTO dto = urgentDto();
        dto.setLatitude(null);
        dto.setLongitude(null);

        assertThatThrownBy(() -> service.create(dto, CLIENT))
                .isInstanceOf(InvalidRequestDataException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void scheduledRequestWithoutAddressIsRejected() {
        CreateRequestDTO dto = scheduledDto();
        dto.setAddress("  ");

        assertThatThrownBy(() -> service.create(dto, CLIENT))
                .isInstanceOf(InvalidRequestDataException.class);
    }

    @Test
    void scheduledRequestWithoutZoneIsRejected() {
        CreateRequestDTO dto = scheduledDto();
        dto.setZoneId(null);

        assertThatThrownBy(() -> service.create(dto, CLIENT))
                .isInstanceOfSatisfying(InvalidRequestDataException.class,
                        ex -> assertThat(ex.getDetails()).containsKey("zoneId"));
        verify(repository, never()).save(any());
    }

    @Test
    void urgentRequestDoesNotNeedZone() {
        CreateRequestDTO dto = urgentDto();
        dto.setZoneId(null);

        assertThat(service.create(dto, CLIENT).getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void urgentModeIsRejectedWhenServiceTypeDoesNotAllowIt() {
        when(catalogClient.findServiceType(serviceTypeId)).thenReturn(Optional.of(serviceType(true, false)));

        assertThatThrownBy(() -> service.create(urgentDto(), CLIENT))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("URGENT_MODE_NOT_ALLOWED");
    }

    @Test
    void inactiveServiceTypeIsRejected() {
        when(catalogClient.findServiceType(serviceTypeId)).thenReturn(Optional.of(serviceType(false, true)));

        assertThatThrownBy(() -> service.create(urgentDto(), CLIENT))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("SERVICE_TYPE_INACTIVE");
    }

    @Test
    void unknownServiceTypeIsRejected() {
        when(catalogClient.findServiceType(serviceTypeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(urgentDto(), CLIENT))
                .isInstanceOf(InvalidRequestDataException.class);
    }

    @Test
    void acceptedRequestCannotBeUpdated() {
        ServiceRequest existing = existing(RequestStatus.ACCEPTED);
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setDescription("Nueva descripción");
        dto.setLatitude(new BigDecimal("2.927300"));
        dto.setLongitude(new BigDecimal("-75.281900"));

        assertThatThrownBy(() -> service.update(existing.getId(), dto, CLIENT))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("REQUEST_NOT_EDITABLE");
    }

    @Test
    void openRequestIsUpdated() {
        ServiceRequest existing = existing(RequestStatus.PENDING);
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setDescription("  Ahora tampoco prende el radio ");
        dto.setLatitude(new BigDecimal("2.930000"));
        dto.setLongitude(new BigDecimal("-75.280000"));
        dto.setVehiclePlate("xyz12d");

        RequestResponseDTO result = service.update(existing.getId(), dto, CLIENT);

        assertThat(result.getDescription()).isEqualTo("Ahora tampoco prende el radio");
        assertThat(result.getVehiclePlate()).isEqualTo("XYZ12D");
    }

    @Test
    void updateWithOnlyLatitudeIsRejected() {
        ServiceRequest existing = existing(RequestStatus.PENDING);
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setDescription("Nueva descripción");
        dto.setLatitude(new BigDecimal("2.930000"));

        assertThatThrownBy(() -> service.update(existing.getId(), dto, CLIENT))
                .isInstanceOf(InvalidRequestDataException.class);
    }

    @Test
    void openRequestIsCancelledWithReason() {
        ServiceRequest existing = existing(RequestStatus.OPEN);

        RequestResponseDTO result = service.cancel(existing.getId(), new CancelRequestDTO("Ya lo solucioné"), CLIENT);

        assertThat(result.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        assertThat(result.getCancellationReason()).isEqualTo("Ya lo solucioné");
        assertThat(result.getCancelledAt()).isEqualTo(NOW);
        verify(notificationClient).notifyStatusChanged(existing);
    }

    @Test
    void findByIdReturnsTheRequest() {
        ServiceRequest existing = existing(RequestStatus.PENDING);

        assertThat(service.findById(existing.getId(), CLIENT).getId()).isEqualTo(existing.getId());
    }

    @Test
    void createTakesTheClientFromTheActor() {
        CreateRequestDTO dto = scheduledDto();

        service.create(dto, CLIENT);

        ArgumentCaptor<ServiceRequest> captor = ArgumentCaptor.forClass(ServiceRequest.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getClientId()).isEqualTo(55L);
    }

    @ParameterizedTest
    @EnumSource(value = ActorRole.class, names = {"PROVIDER", "ADMIN", "SERVICE"})
    void onlyClientsCanCreate(ActorRole role) {
        assertThatThrownBy(() -> service.create(urgentDto(), new CurrentActor(9L, role)))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void clientListsOnlyOwnRequests() {
        ServiceRequest open = existing(RequestStatus.OPEN);
        when(repository.findByClientIdOrderByCreatedAtDesc(55L)).thenReturn(List.of(open));
        when(repository.findByClientIdAndStatusOrderByCreatedAtDesc(55L, RequestStatus.OPEN))
                .thenReturn(List.of(open));

        assertThat(service.findAll(null, null, CLIENT)).hasSize(1);
        assertThat(service.findAll(55L, RequestStatus.OPEN, CLIENT)).hasSize(1);
    }

    @Test
    void clientCannotListAnotherClientsRequests() {
        assertThatThrownBy(() -> service.findAll(56L, null, CLIENT))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void providerListsOnlyAssignedRequests() {
        ServiceRequest accepted = existing(RequestStatus.ACCEPTED);
        when(repository.findByProviderIdOrderByCreatedAtDesc(77L)).thenReturn(List.of(accepted));
        when(repository.findByProviderIdAndStatusOrderByCreatedAtDesc(77L, RequestStatus.ACCEPTED))
                .thenReturn(List.of(accepted));

        assertThat(service.findAll(null, null, PROVIDER)).hasSize(1);
        assertThat(service.findAll(null, RequestStatus.ACCEPTED, PROVIDER)).hasSize(1);
        assertThatThrownBy(() -> service.findAll(55L, null, PROVIDER))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void adminListsEverythingWithOptionalFilters() {
        ServiceRequest open = existing(RequestStatus.OPEN);
        ServiceRequest pending = existing(RequestStatus.PENDING);
        when(repository.findByClientIdAndStatusOrderByCreatedAtDesc(55L, RequestStatus.OPEN))
                .thenReturn(List.of(open));
        when(repository.findByClientIdOrderByCreatedAtDesc(55L)).thenReturn(List.of(open, pending));
        when(repository.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING)).thenReturn(List.of(pending));
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        assertThat(service.findAll(55L, RequestStatus.OPEN, ADMIN)).hasSize(1);
        assertThat(service.findAll(55L, null, ADMIN)).hasSize(2);
        assertThat(service.findAll(null, RequestStatus.PENDING, ADMIN)).hasSize(1);
        assertThat(service.findAll(null, null, ADMIN)).isEmpty();
    }

    @Test
    void internalServicesCannotListRequests() {
        assertThatThrownBy(() -> service.findAll(null, null, SERVICE))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void ownerAssignedProviderAndAdminCanReadARequest() {
        ServiceRequest accepted = existing(RequestStatus.ACCEPTED);
        accepted.setProviderId(77L);

        assertThat(service.findById(accepted.getId(), CLIENT)).isNotNull();
        assertThat(service.findById(accepted.getId(), PROVIDER)).isNotNull();
        assertThat(service.findById(accepted.getId(), ADMIN)).isNotNull();
    }

    @Test
    void strangersCannotReadARequest() {
        ServiceRequest accepted = existing(RequestStatus.ACCEPTED);
        accepted.setProviderId(77L);

        assertThatThrownBy(() -> service.findById(accepted.getId(), OTHER_CLIENT))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.findById(accepted.getId(), new CurrentActor(78L, ActorRole.PROVIDER)))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.findById(accepted.getId(), SERVICE))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void onlyTheOwnerClientCanUpdate() {
        ServiceRequest existing = existing(RequestStatus.PENDING);
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setDescription("Nueva descripción");
        dto.setLatitude(new BigDecimal("2.930000"));
        dto.setLongitude(new BigDecimal("-75.280000"));

        assertThatThrownBy(() -> service.update(existing.getId(), dto, OTHER_CLIENT))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.update(existing.getId(), dto, ADMIN))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.update(existing.getId(), dto, PROVIDER))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(repository, never()).save(existing);
    }

    @Test
    void vehicleDataIsRequiredWhenTheServiceTypeAsksForIt() {
        when(catalogClient.findServiceType(serviceTypeId)).thenReturn(Optional.of(vehicleServiceType()));
        CreateRequestDTO dto = urgentDto();
        dto.setVehiclePlate(null);

        assertThatThrownBy(() -> service.create(dto, CLIENT))
                .isInstanceOfSatisfying(InvalidRequestDataException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("VEHICLE_DATA_REQUIRED"));
        verify(repository, never()).save(any());
    }

    @Test
    void vehicleDataIsAcceptedWhenComplete() {
        when(catalogClient.findServiceType(serviceTypeId)).thenReturn(Optional.of(vehicleServiceType()));
        CreateRequestDTO dto = urgentDto();
        dto.setVehicleBrand("Chevrolet");
        dto.setVehicleType("Automóvil");

        assertThat(service.create(dto, CLIENT).getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void updateKeepsRequiringVehicleData() {
        when(catalogClient.findServiceType(serviceTypeId)).thenReturn(Optional.of(vehicleServiceType()));
        ServiceRequest existing = existing(RequestStatus.PENDING);
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setDescription("Nueva descripción");
        dto.setLatitude(new BigDecimal("2.930000"));
        dto.setLongitude(new BigDecimal("-75.280000"));

        assertThatThrownBy(() -> service.update(existing.getId(), dto, CLIENT))
                .isInstanceOfSatisfying(InvalidRequestDataException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("VEHICLE_DATA_REQUIRED"));
        verify(repository, never()).save(existing);
    }

    @Test
    void updateFailsWhenTheServiceTypeNoLongerExists() {
        ServiceRequest existing = existing(RequestStatus.PENDING);
        when(catalogClient.findServiceType(serviceTypeId)).thenReturn(Optional.empty());
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setDescription("Nueva descripción");
        dto.setLatitude(new BigDecimal("2.930000"));
        dto.setLongitude(new BigDecimal("-75.280000"));

        assertThatThrownBy(() -> service.update(existing.getId(), dto, CLIENT))
                .isInstanceOf(InvalidRequestDataException.class);
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"PENDING", "OPEN", "ACCEPTED", "ON_THE_WAY"})
    void clientCancelsBeforeTheServiceIsInProgress(RequestStatus status) {
        ServiceRequest existing = existing(status);

        assertThat(service.cancel(existing.getId(), new CancelRequestDTO("Cambio de planes"), CLIENT).getStatus())
                .isEqualTo(RequestStatus.CANCELLED);
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"IN_PROGRESS", "COMPLETED", "CANCELLED", "EXPIRED"})
    void clientCannotCancelFromTheseStates(RequestStatus status) {
        ServiceRequest existing = existing(status);

        assertThatThrownBy(() -> service.cancel(existing.getId(), new CancelRequestDTO("Cambio de planes"), CLIENT))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("REQUEST_NOT_CANCELLABLE");
        verify(repository, never()).save(existing);
    }

    @Test
    void assignedProviderCancelsOnlyWhileAcceptedOrOnTheWay() {
        ServiceRequest accepted = existing(RequestStatus.ACCEPTED);
        accepted.setProviderId(77L);
        ServiceRequest inProgress = existing(RequestStatus.IN_PROGRESS);
        inProgress.setProviderId(77L);

        assertThat(service.cancel(accepted.getId(), new CancelRequestDTO("Imprevisto"), PROVIDER).getStatus())
                .isEqualTo(RequestStatus.CANCELLED);
        assertThatThrownBy(() -> service.cancel(inProgress.getId(), new CancelRequestDTO("Imprevisto"), PROVIDER))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("REQUEST_NOT_CANCELLABLE");
    }

    @Test
    void adminAndSystemCancelEvenWhileInProgress() {
        ServiceRequest first = existing(RequestStatus.IN_PROGRESS);
        ServiceRequest second = existing(RequestStatus.IN_PROGRESS);

        assertThat(service.cancel(first.getId(), new CancelRequestDTO("Suspensión de la cuenta"), ADMIN).getStatus())
                .isEqualTo(RequestStatus.CANCELLED);
        assertThat(service.cancel(second.getId(), new CancelRequestDTO("Verificación revocada"), SERVICE).getStatus())
                .isEqualTo(RequestStatus.CANCELLED);
    }

    @Test
    void finalRequestsCannotBeCancelledEvenByAdmin() {
        ServiceRequest completed = existing(RequestStatus.COMPLETED);

        assertThatThrownBy(() -> service.cancel(completed.getId(), new CancelRequestDTO("Error"), ADMIN))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("REQUEST_NOT_CANCELLABLE");
    }

    @Test
    void providerThatIsNotAssignedCannotCancel() {
        ServiceRequest accepted = existing(RequestStatus.ACCEPTED);
        accepted.setProviderId(78L);

        assertThatThrownBy(() -> service.cancel(accepted.getId(), new CancelRequestDTO("Imprevisto"), PROVIDER))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void strangersCannotCancel() {
        ServiceRequest existing = existing(RequestStatus.OPEN);

        assertThatThrownBy(() -> service.cancel(existing.getId(), new CancelRequestDTO("Motivo"), OTHER_CLIENT))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(repository, never()).save(existing);
    }

    @Test
    void adminCanCancel() {
        ServiceRequest existing = existing(RequestStatus.OPEN);

        assertThat(service.cancel(existing.getId(), new CancelRequestDTO("Cuenta suspendida"), ADMIN).getStatus())
                .isEqualTo(RequestStatus.CANCELLED);
    }
    @Test
    void unknownRequestReturnsNotFound() {
        Long id = 999L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id, CLIENT))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private ServiceTypeInfo vehicleServiceType() {
        return new ServiceTypeInfo(serviceTypeId, true, true, new BigDecimal("85000.00"), false, true);
    }

    private ServiceTypeInfo serviceType(boolean active, boolean urgentAllowed) {
        return new ServiceTypeInfo(serviceTypeId, active, urgentAllowed, new BigDecimal("85000.00"), false, false);
    }

    private CreateRequestDTO urgentDto() {
        CreateRequestDTO dto = new CreateRequestDTO();
        dto.setServiceTypeId(serviceTypeId);
        dto.setMode(RequestMode.URGENT);
        dto.setZoneId(3L);
        dto.setDescription("El carro no enciende");
        dto.setLatitude(new BigDecimal("2.927300"));
        dto.setLongitude(new BigDecimal("-75.281900"));
        dto.setVehiclePlate("abc123");
        return dto;
    }

    private CreateRequestDTO scheduledDto() {
        CreateRequestDTO dto = new CreateRequestDTO();
        dto.setServiceTypeId(serviceTypeId);
        dto.setMode(RequestMode.SCHEDULED);
        dto.setZoneId(3L);
        dto.setDescription("Revisar la nevera, no enfría");
        dto.setAddress("Calle 10 # 5-20, Neiva");
        return dto;
    }

    private ServiceRequest existing(RequestStatus status) {
        ServiceRequest request = new ServiceRequest();
        request.setId(ids.incrementAndGet());
        request.setClientId(55L);
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
