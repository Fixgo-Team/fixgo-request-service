package co.fixgo.request.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.fixgo.request.dto.CreateRequestDTO;
import co.fixgo.request.dto.RequestResponseDTO;
import co.fixgo.request.exceptions.GlobalExceptionHandler;
import co.fixgo.request.exceptions.ResourceNotFoundException;
import co.fixgo.request.models.RequestStatus;
import co.fixgo.request.services.RequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Contrato HTTP del controlador con el servicio simulado: códigos, wrapper y cabecera Location.
 */
class RequestControllerTest {

    private RequestService requestService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        requestService = mock(RequestService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RequestController(requestService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        RequestResponseDTO created = new RequestResponseDTO();
        created.setId(42L);
        created.setStatus(RequestStatus.PENDING);
        when(requestService.create(any(CreateRequestDTO.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"serviceTypeId\":7,\"mode\":\"URGENT\","
                                + "\"description\":\"El carro no enciende\","
                                + "\"latitude\":2.9273,\"longitude\":-75.2819}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/requests/42"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(42));
    }

    @Test
    void createWithoutRequiredFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.serviceTypeId").exists());
    }

    @Test
    void findByIdReturns404WhenMissing() throws Exception {
        when(requestService.findById(9L)).thenThrow(new ResourceNotFoundException("La solicitud 9 no existe."));

        mockMvc.perform(get("/api/v1/requests/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void idThatIsNotNumericReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/requests/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }
}
