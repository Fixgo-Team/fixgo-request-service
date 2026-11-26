package co.fixgo.request.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import co.fixgo.request.dto.CreateRequestDTO;
import co.fixgo.request.dto.RequestResponseDTO;
import co.fixgo.request.models.RequestMode;
import co.fixgo.request.models.RequestStatus;
import co.fixgo.request.models.ServiceRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestMapperTest {

    private final RequestMapper mapper = new RequestMapper();

    @Test
    void toEntityCleansTextAndUppercasesThePlate() {
        CreateRequestDTO dto = new CreateRequestDTO();
        dto.setClientId(UUID.randomUUID());
        dto.setServiceTypeId(UUID.randomUUID());
        dto.setMode(RequestMode.URGENT);
        dto.setDescription("  El carro no enciende  ");
        dto.setAddress("   ");
        dto.setVehiclePlate(" abc123 ");
        dto.setVehicleBrand(" Chevrolet ");
        dto.setVehicleType("Automóvil");

        ServiceRequest request = mapper.toEntity(dto);

        assertThat(request.getDescription()).isEqualTo("El carro no enciende");
        assertThat(request.getAddress()).isNull();
        assertThat(request.getVehiclePlate()).isEqualTo("ABC123");
        assertThat(request.getVehicleBrand()).isEqualTo("Chevrolet");
        assertThat(request.getVehicleType()).isEqualTo("Automóvil");
    }

    @Test
    void toResponseCopiesEveryFieldAndConvertsDatesToInstants() {
        ServiceRequest request = new ServiceRequest();
        request.setId(UUID.randomUUID());
        request.setClientId(UUID.randomUUID());
        request.setServiceTypeId(UUID.randomUUID());
        request.setProviderId(UUID.randomUUID());
        request.setMode(RequestMode.SCHEDULED);
        request.setStatus(RequestStatus.CANCELLED);
        request.setDescription("Revisar la nevera");
        request.setAddress("Calle 10 # 5-20");
        request.setLatitude(new BigDecimal("2.927300"));
        request.setLongitude(new BigDecimal("-75.281900"));
        request.setCancellationReason("Ya no lo necesito");
        request.setCancelledAt(LocalDateTime.of(2026, 11, 12, 1, 30));
        request.setCreatedAt(LocalDateTime.of(2026, 11, 12, 1, 0));
        request.setUpdatedAt(LocalDateTime.of(2026, 11, 12, 1, 30));

        RequestResponseDTO dto = mapper.toResponse(request);

        assertThat(dto.getId()).isEqualTo(request.getId());
        assertThat(dto.getProviderId()).isEqualTo(request.getProviderId());
        assertThat(dto.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        assertThat(dto.getAddress()).isEqualTo("Calle 10 # 5-20");
        assertThat(dto.getCancelledAt()).hasToString("2026-11-12T01:30:00Z");
        assertThat(dto.getCreatedAt()).hasToString("2026-11-12T01:00:00Z");
        assertThat(dto.getUpdatedAt()).hasToString("2026-11-12T01:30:00Z");
    }
}
