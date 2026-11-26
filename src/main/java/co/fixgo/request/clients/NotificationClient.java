package co.fixgo.request.clients;

import co.fixgo.request.models.ServiceRequest;
import java.util.List;

/**
 * Lo que este servicio le pide a fixgo-notification-service (RF5.1).
 * Un fallo al notificar no debe perder la solicitud (RNF-12): las implementaciones no lanzan excepciones.
 */
public interface NotificationClient {

    /** Avisa que se creó una solicitud nueva. */
    void notifyRequestCreated(ServiceRequest request);

    /** Avisa que una solicitud cambió de estado. */
    void notifyStatusChanged(ServiceRequest request);

    /** Avisa a los profesionales candidatos de una solicitud programada nueva (RN-02). */
    void notifyProviders(ServiceRequest request, List<Long> providerIds);
}
