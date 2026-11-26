package co.fixgo.request.clients;

import java.util.Optional;

/**
 * Lo que este servicio le pide a catalog-provider-service. Nunca se leen sus tablas (RNF-22):
 * la implementación real llamará por REST a su API.
 */
public interface CatalogClient {

    /** Busca un subservicio; vacío si no existe. */
    Optional<ServiceTypeInfo> findServiceType(Long serviceTypeId);
}
