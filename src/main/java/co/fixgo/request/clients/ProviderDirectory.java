package co.fixgo.request.clients;

import java.util.List;

/**
 * Lo que este servicio le pide a catalog-provider-service (módulo provider-service) para encontrar a los
 * profesionales a quienes avisar de una solicitud programada (RN-02): verificados y que ofrecen el subservicio
 * en la zona. Nunca se leen sus tablas (RNF-22).
 */
public interface ProviderDirectory {

    /**
     * @return ids de los profesionales que cumplen; lista vacía si no hay
     * @throws co.fixgo.request.exceptions.DependencyUnavailableException si no responde a tiempo
     */
    List<Long> findProvidersFor(Long serviceTypeId, Long zoneId);
}
