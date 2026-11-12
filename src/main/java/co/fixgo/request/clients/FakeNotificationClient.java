package co.fixgo.request.clients;

import co.fixgo.request.models.ServiceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementación TEMPORAL mientras fixgo-notification-service publica su API: solo escribe en el log.
 * Se reemplaza por NotificationRestClient sin tocar RequestServiceImpl.
 */
@Component
public class FakeNotificationClient implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(FakeNotificationClient.class);

    @Override
    public void notifyRequestCreated(ServiceRequest request) {
        log.info("Notificación simulada: nueva solicitud {} ({}) para el subservicio {}",
                request.getId(), request.getMode(), request.getServiceTypeId());
    }

    @Override
    public void notifyStatusChanged(ServiceRequest request) {
        log.info("Notificación simulada: la solicitud {} pasó a {}", request.getId(), request.getStatus());
    }
}
