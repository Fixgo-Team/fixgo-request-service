package co.fixgo.request.clients;

import java.math.BigDecimal;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementación TEMPORAL mientras catalog-provider-service publica su API.
 * Responde que todo subservicio existe, está activo, admite modo urgente y cuesta 50.000 COP de referencia;
 * no exige confirmar propiedad ni datos del vehículo.
 * Se reemplaza por CatalogRestClient sin tocar RequestServiceImpl.
 */
@Component
public class FakeCatalogClient implements CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(FakeCatalogClient.class);

    private static final BigDecimal REFERENCE_PRICE = new BigDecimal("50000.00");

    @Override
    public Optional<ServiceTypeInfo> findServiceType(Long serviceTypeId) {
        log.warn("Catálogo simulado: se asume que el subservicio {} existe y admite urgencias", serviceTypeId);
        return Optional.of(new ServiceTypeInfo(serviceTypeId, true, true, REFERENCE_PRICE, false, false));
    }
}
