package co.fixgo.request.clients;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementación TEMPORAL mientras catalog-provider-service publica su API: no encuentra profesionales.
 * Se reemplaza por ProviderDirectoryRestClient sin tocar RequestServiceImpl.
 */
@Component
public class FakeProviderDirectory implements ProviderDirectory {

    private static final Logger log = LoggerFactory.getLogger(FakeProviderDirectory.class);

    @Override
    public List<Long> findProvidersFor(Long serviceTypeId, Long zoneId) {
        log.warn("Directorio de profesionales simulado: sin candidatos para el subservicio {} en la zona {}",
                serviceTypeId, zoneId);
        return List.of();
    }
}
