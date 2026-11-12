package co.fixgo.request.clients;

import java.util.Optional;
import java.util.UUID;

/**
 * Lo que este servicio le pide a fixgo-catalog-service. Nunca se leen sus tablas (RNF-14):
 * la implementación real llamará por REST a su API.
 */
public interface CatalogClient {

    /** Busca un subservicio; vacío si no existe. */
    Optional<ServiceTypeInfo> findServiceType(UUID serviceTypeId);
}
