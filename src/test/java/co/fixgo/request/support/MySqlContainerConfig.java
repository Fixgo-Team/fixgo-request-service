package co.fixgo.request.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;

/**
 * MySQL 8 real en un contenedor para las pruebas de integración (BD-01).
 * Spring toma la URL y las credenciales del contenedor con @ServiceConnection; las migraciones de Flyway corren sobre él.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MySqlContainerConfig {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer("mysql:8.4")
                .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");
    }
}
