package co.fixgo.request.clients;

import co.fixgo.request.models.ServiceRequest;

/**
 * Lo que este servicio le pide a fixgo-notification-service (RF5.1).
 * Un fallo al notificar no debe perder la solicitud (RNF-08): las implementaciones no lanzan excepciones.
 */
public interface NotificationClient {

    /** Avisa que se creó una solicitud nueva. */
    void notifyRequestCreated(ServiceRequest request);

    /** Avisa que una solicitud cambió de estado. */
    void notifyStatusChanged(ServiceRequest request);
}
