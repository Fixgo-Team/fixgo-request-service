package co.fixgo.request.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.fixgo.request.clients.CatalogClient;
import co.fixgo.request.clients.NotificationClient;
import co.fixgo.request.clients.ServiceTypeInfo;
import co.fixgo.request.config.ClockConfig;
import co.fixgo.request.config.SecurityConfig;
import co.fixgo.request.mappers.RequestMapper;
import co.fixgo.request.models.RequestMode;
import co.fixgo.request.models.RequestStatus;
import co.fixgo.request.models.ServiceRequest;
import co.fixgo.request.policies.RequestTransitionPolicy;
import co.fixgo.request.policies.VehicleDataPolicy;
import co.fixgo.request.repositories.ServiceRequestRepository;
import co.fixgo.request.security.ApiSecurityErrorHandler;
import co.fixgo.request.security.JwtCurrentActorProvider;
import co.fixgo.request.services.impl.RequestServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato HTTP y seguridad del controlador con la cadena de filtros real (JWT simulado con jwt()),
 * el servicio real y sus dependencias simuladas: 401 sin token, 403 por rol o por propiedad, 201 con Location.
 */
@WebMvcTest(RequestController.class)
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, JwtCurrentActorProvider.class,
        RequestServiceImpl.class, RequestMapper.class, RequestTransitionPolicy.class, VehicleDataPolicy.class,
        ClockConfig.class})
class RequestControllerTest {

    private static final String URGENT_BODY = "{\"serviceTypeId\":7,\"mode\":\"URGENT\","
            + "\"description\":\"El carro no enciende\",\"latitude\":2.9273,\"longitude\":-75.2819}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceRequestRepository repository;

    @MockitoBean
    private CatalogClient catalogClient;

    @MockitoBean
    private NotificationClient notificationClient;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        when(catalogClient.findServiceType(7L)).thenReturn(Optional.of(
                new ServiceTypeInfo(7L, true, true, new BigDecimal("50000.00"), false, false)));
        when(repository.save(any(ServiceRequest.class))).thenAnswer(invocation -> {
            ServiceRequest saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });
    }

    @Test
    void withoutTokenReturns401InsideTheApiWrapper() throws Exception {
        mockMvc.perform(get("/api/v1/requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON).content(URGENT_BODY))
                .andExpect(status().isUnauthorized());
        verify(repository, never()).save(any());
    }

    @Test
    void clientCreatesAUrgentRequestAndGetsLocationHeader() throws Exception {
        mockMvc.perform(post("/api/v1/requests").with(user(55L, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(URGENT_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/requests/42"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.clientId").value(55))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.agreedValue").value(50000.00));
    }

    @Test
    void clientIdInTheBodyIsIgnored() throws Exception {
        String body = URGENT_BODY.replace("{", "{\"clientId\":999,");

        mockMvc.perform(post("/api/v1/requests").with(user(55L, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.clientId").value(55));
    }

    @Test
    void wrongRoleCannotCreate() throws Exception {
        for (String role : List.of("PROVIDER", "ADMIN", "SERVICE")) {
            mockMvc.perform(post("/api/v1/requests").with(user(55L, role))
                            .contentType(MediaType.APPLICATION_JSON).content(URGENT_BODY))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }
        verify(repository, never()).save(any());
    }

    @Test
    void tokenWithoutRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/requests").with(jwt().jwt(token -> token.claim("userId", 55))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void invalidBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/requests").with(user(55L, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.serviceTypeId").exists());
    }

    @Test
    void ownerReadsHisRequest() throws Exception {
        stored(10L, 55L, null);

        mockMvc.perform(get("/api/v1/requests/10").with(user(55L, "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void assignedProviderAndAdminReadTheRequest() throws Exception {
        stored(10L, 55L, 77L);

        mockMvc.perform(get("/api/v1/requests/10").with(user(77L, "PROVIDER"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/requests/10").with(user(1L, "ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void anotherClientCannotReadTheRequest() throws Exception {
        stored(10L, 55L, 77L);

        mockMvc.perform(get("/api/v1/requests/10").with(user(56L, "CLIENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/v1/requests/10").with(user(78L, "PROVIDER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownRequestReturns404() throws Exception {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/requests/9").with(user(55L, "CLIENT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void idThatIsNotNumericReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/requests/abc").with(user(55L, "CLIENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void clientListsOnlyHisRequestsAndCannotAskForOthers() throws Exception {
        ServiceRequest mine = stored(10L, 55L, null);
        when(repository.findByClientIdOrderByCreatedAtDesc(55L)).thenReturn(List.of(mine));

        mockMvc.perform(get("/api/v1/requests").with(user(55L, "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get("/api/v1/requests").param("clientId", "56").with(user(55L, "CLIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anotherClientCannotUpdateNorCancel() throws Exception {
        stored(10L, 55L, null);

        mockMvc.perform(put("/api/v1/requests/10").with(user(56L, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Cambio\",\"latitude\":2.9,\"longitude\":-75.2}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/requests/10/cancel").with(user(56L, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Motivo\"}"))
                .andExpect(status().isForbidden());
        verify(repository, never()).save(any());
    }

    @Test
    void ownerUpdatesAndCancels() throws Exception {
        stored(10L, 55L, null);

        mockMvc.perform(put("/api/v1/requests/10").with(user(55L, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Cambio\",\"latitude\":2.9,\"longitude\":-75.2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("Cambio"));
        mockMvc.perform(patch("/api/v1/requests/10/cancel").with(user(55L, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Ya lo solucioné\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void clientCannotCancelAServiceInProgressButAdminCan() throws Exception {
        ServiceRequest inProgress = stored(10L, 55L, 77L);
        inProgress.setStatus(RequestStatus.IN_PROGRESS);

        mockMvc.perform(patch("/api/v1/requests/10/cancel").with(user(55L, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Motivo\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REQUEST_NOT_CANCELLABLE"));
        mockMvc.perform(patch("/api/v1/requests/10/cancel").with(user(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Cuenta suspendida\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void vehicleDataIsRequiredForServicesThatAskForIt() throws Exception {
        when(catalogClient.findServiceType(7L)).thenReturn(Optional.of(
                new ServiceTypeInfo(7L, true, true, new BigDecimal("50000.00"), false, true)));

        mockMvc.perform(post("/api/v1/requests").with(user(55L, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(URGENT_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_DATA_REQUIRED"))
                .andExpect(jsonPath("$.error.details.vehiclePlate").exists())
                .andExpect(jsonPath("$.error.details.vehicleBrand").exists())
                .andExpect(jsonPath("$.error.details.vehicleType").exists());
    }

    @Test
    void swaggerAndHealthArePublicButOtherActuatorEndpointsAreForAdmins() throws Exception {
        // Sin controlador en el slice, una ruta pública responde 404 en vez de 401
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isNotFound());

        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").with(user(55L, "CLIENT"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/metrics").with(user(1L, "ADMIN"))).andExpect(status().isNotFound());
    }

    private JwtRequestPostProcessor user(Long userId, String role) {
        return jwt().jwt(token -> token.claim("userId", userId).claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private ServiceRequest stored(Long id, Long clientId, Long providerId) {
        ServiceRequest request = new ServiceRequest();
        request.setId(id);
        request.setClientId(clientId);
        request.setProviderId(providerId);
        request.setServiceTypeId(7L);
        request.setMode(RequestMode.URGENT);
        request.setStatus(RequestStatus.PENDING);
        request.setDescription("Solicitud existente");
        request.setLatitude(new BigDecimal("2.927300"));
        request.setLongitude(new BigDecimal("-75.281900"));
        when(repository.findById(id)).thenReturn(Optional.of(request));
        return request;
    }
}
