package co.fixgo.request.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import co.fixgo.request.dto.CreateRequestDTO;
import co.fixgo.request.dto.RequestResponseDTO;
import co.fixgo.request.dto.UpdateRequestDTO;
import co.fixgo.request.models.RequestMode;
import co.fixgo.request.models.RequestStatus;
import co.fixgo.request.models.ServiceRequest;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RequestMapperTest {

    private final RequestMapper mapper = new RequestMapper();

    @Test
    void toEntityCleansTextAndUppercasesThePlate() {
        CreateRequestDTO dto = new CreateRequestDTO();
        dto.setServiceTypeId(2L);
        dto.setZoneId(3L);
        dto.setMode(RequestMode.URGENT);
        dto.setDescription("  El carro no enciende  ");
        dto.setAddress("   ");
        dto.setVehiclePlate(" abc123 ");
        dto.setVehicleBrand(" Chevrolet ");
        dto.setVehicleType("Automóvil");

        ServiceRequest request = mapper.toEntity(dto);

        assertThat(request.getZoneId()).isEqualTo(3L);
        assertThat(request.getDescription()).isEqualTo("El carro no enciende");
        assertThat(request.getAddress()).isNull();
        assertThat(request.getVehiclePlate()).isEqualTo("ABC123");
        assertThat(request.getVehicleBrand()).isEqualTo("Chevrolet");
        assertThat(request.getVehicleType()).isEqualTo("Automóvil");
    }

    @Test
    void applyUpdateCopiesTheEditableFields() {
        ServiceRequest request = new ServiceRequest();
        request.setClientId(1L);
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setZoneId(9L);
        dto.setDescription(" Nueva descripción ");
        dto.setAddress("Carrera 7 # 3-10");

        mapper.applyUpdate(request, dto);

        assertThat(request.getClientId()).isEqualTo(1L);
        assertThat(request.getZoneId()).isEqualTo(9L);
        assertThat(request.getDescription()).isEqualTo("Nueva descripción");
        assertThat(request.getAddress()).isEqualTo("Carrera 7 # 3-10");
    }

    @Test
    void toResponseCopiesEveryField() {
        ServiceRequest request = new ServiceRequest();
        request.setId(10L);
        request.setClientId(1L);
        request.setServiceTypeId(2L);
        request.setZoneId(3L);
        request.setProviderId(4L);
        request.setMode(RequestMode.SCHEDULED);
        request.setStatus(RequestStatus.CANCELLED);
        request.setDescription("Revisar la nevera");
        request.setAddress("Calle 10 # 5-20");
        request.setLatitude(new BigDecimal("2.927300"));
        request.setLongitude(new BigDecimal("-75.281900"));
        request.setAgreedValue(new BigDecimal("120000.00"));
        request.setCancellationReason("Ya no lo necesito");
        request.setCancelledAt(Instant.parse("2026-11-12T01:30:00Z"));
        request.setCreatedAt(Instant.parse("2026-11-12T01:00:00Z"));
        request.setUpdatedAt(Instant.parse("2026-11-12T01:30:00Z"));

        RequestResponseDTO dto = mapper.toResponse(request);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getClientId()).isEqualTo(1L);
        assertThat(dto.getServiceTypeId()).isEqualTo(2L);
        assertThat(dto.getZoneId()).isEqualTo(3L);
        assertThat(dto.getProviderId()).isEqualTo(4L);
        assertThat(dto.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        assertThat(dto.getAddress()).isEqualTo("Calle 10 # 5-20");
        assertThat(dto.getAgreedValue()).isEqualByComparingTo("120000.00");
        assertThat(dto.getCancelledAt()).hasToString("2026-11-12T01:30:00Z");
        assertThat(dto.getCreatedAt()).hasToString("2026-11-12T01:00:00Z");
        assertThat(dto.getUpdatedAt()).hasToString("2026-11-12T01:30:00Z");
    }
}
