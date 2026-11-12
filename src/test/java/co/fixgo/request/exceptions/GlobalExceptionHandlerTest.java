package co.fixgo.request.exceptions;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.fixgo.request.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifica la lista de chequeo de la guía del wrapper: el código HTTP coincide con el JSON,
 * success es true/false según el caso y cada campo inválido aparece en details.
 * Usa un controlador de prueba, sin levantar Spring ni la base de datos.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returns201WithSuccessTrueWhenBodyIsValid() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Cambio de guarda\",\"price\":80000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.description").value("Cambio de guarda"))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.meta.apiVersion").value("v1.0"));
    }

    @Test
    void returns400WithEveryInvalidFieldInDetails() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"\",\"price\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.description").value("La descripción es obligatoria."))
                .andExpect(jsonPath("$.error.details.price").value("El precio debe ser mayor que cero."));
    }

    @Test
    void returns400WhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{description"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void returns404WhenResourceDoesNotExist() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("La solicitud no existe."));
    }

    @Test
    void returns409WithRuleCodeWhenBusinessRuleIsBroken() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("QUOTE_ALREADY_ACCEPTED"));
    }

    @Test
    void returns500WithoutInternalDetails() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Ocurrió un error inesperado. Intenta de nuevo más tarde."));
    }

    public static class TestBody {

        @NotBlank(message = "La descripción es obligatoria.")
        private String description;

        @Positive(message = "El precio debe ser mayor que cero.")
        private BigDecimal price;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }

    @RestController
    static class TestController {

        @PostMapping("/test/body")
        ResponseEntity<ApiResponse<TestBody>> body(@Valid @RequestBody TestBody body) {
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(body));
        }

        @GetMapping("/test/not-found")
        ResponseEntity<ApiResponse<Void>> notFound() {
            throw new ResourceNotFoundException("La solicitud no existe.");
        }

        @GetMapping("/test/conflict")
        ResponseEntity<ApiResponse<Void>> conflict() {
            throw new BusinessRuleException("QUOTE_ALREADY_ACCEPTED", "La solicitud ya tiene una cotización aceptada.");
        }

        @GetMapping("/test/unexpected")
        ResponseEntity<ApiResponse<Void>> unexpected() {
            throw new IllegalStateException("fallo interno");
        }
    }
}
