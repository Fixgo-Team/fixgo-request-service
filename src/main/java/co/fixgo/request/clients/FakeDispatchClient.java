package co.fixgo.request.clients;

import co.fixgo.request.models.ServiceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementación TEMPORAL mientras dispatch-matchmaking publica su API: acepta todo y solo escribe en el log.
 * Se reemplaza por DispatchRestClient sin tocar RequestServiceImpl.
 */
@Component
public class FakeDispatchClient implements DispatchClient {

    private static final Logger log = LoggerFactory.getLogger(FakeDispatchClient.class);

    @Override
    public void requestMatching(ServiceRequest request) {
        log.info("Dispatch simulado: emparejamiento registrado para la urgencia {}", request.getId());
    }

    @Override
    public void withdrawOffers(Long requestId) {
        log.info("Dispatch simulado: ofertas retiradas de la solicitud {}", requestId);
    }
}
