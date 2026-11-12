package co.fixgo.request.clients;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementación TEMPORAL mientras fixgo-catalog-service publica su API.
 * Responde que todo subservicio existe, está activo y admite modo urgente.
 * Se reemplaza por CatalogRestClient sin tocar RequestServiceImpl.
 */
@Component
public class FakeCatalogClient implements CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(FakeCatalogClient.class);

    @Override
    public Optional<ServiceTypeInfo> findServiceType(UUID serviceTypeId) {
        log.warn("Catálogo simulado: se asume que el subservicio {} existe y admite urgencias", serviceTypeId);
        return Optional.of(new ServiceTypeInfo(serviceTypeId, true, true));
    }
}
