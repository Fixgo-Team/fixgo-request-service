package co.fixgo.request.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Datos generales del contrato OpenAPI 3 que se publica en Swagger UI (RNF-16).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI requestServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FixGo · Request Service API")
                .version("v1")
                .description("Solicitudes urgentes y programadas, cotizaciones y mensajes (módulo 3 del SRS v3.0)."));
    }
}
