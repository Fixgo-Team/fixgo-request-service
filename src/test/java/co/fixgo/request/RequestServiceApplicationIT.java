package co.fixgo.request;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.fixgo.request.support.MySqlContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Arranca la aplicación completa contra MySQL real: migraciones, validación del esquema por Hibernate,
 * configuración de seguridad con sus valores por defecto y rutas públicas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlContainerConfig.class)
class RequestServiceApplicationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextStartsAndPrivateEndpointsRequireAToken() throws Exception {
        mockMvc.perform(get("/api/v1/requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void healthAndOpenApiArePublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
