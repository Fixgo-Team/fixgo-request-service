package co.fixgo.request;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio de solicitudes de FixGo.
 * Cubre el módulo 3 del SRS v3.0 (solicitudes urgentes y programadas, cotizaciones) y los mensajes (RF5.4).
 */
@SpringBootApplication
public class RequestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RequestServiceApplication.class, args);
    }
}
