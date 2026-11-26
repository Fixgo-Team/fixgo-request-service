package co.fixgo.request.clients;

import co.fixgo.request.models.ServiceRequest;

/**
 * Lo que este servicio le pide a dispatch-matchmaking (RF3.5, RF3.6). El emparejamiento y las ofertas
 * son suyos; aquí solo se le avisa de las urgencias nuevas y de las que dejan de necesitarlo.
 */
public interface DispatchClient {

    /**
     * Pide emparejar una urgencia recién guardada. dispatch resuelve la zona del punto y responde apenas
     * registra el emparejamiento; busca candidatos y envía las ofertas después (RNF-12).
     *
     * @throws co.fixgo.request.exceptions.ZoneNotResolvedException       si no puede resolver la zona
     * @throws co.fixgo.request.exceptions.DependencyUnavailableException si no responde a tiempo
     */
    void requestMatching(ServiceRequest request);

    /**
     * Retira las ofertas abiertas de una urgencia (cancelada o convertida a programada).
     *
     * @throws co.fixgo.request.exceptions.DependencyUnavailableException si no responde a tiempo
     */
    void withdrawOffers(Long requestId);
}
