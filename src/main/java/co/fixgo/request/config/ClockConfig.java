package co.fixgo.request.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj único del servicio, en UTC (BD-02). Las clases de negocio lo reciben por constructor
 * en vez de llamar a Instant.now(), así las pruebas fijan la hora con Clock.fixed.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
