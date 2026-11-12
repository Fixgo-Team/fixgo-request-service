package co.fixgo.request.mappers;

import co.fixgo.request.dto.CreateRequestDTO;
import co.fixgo.request.dto.RequestResponseDTO;
import co.fixgo.request.dto.UpdateRequestDTO;
import co.fixgo.request.models.ServiceRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Convierte entre los DTO de la API y la entidad ServiceRequest. La entidad nunca sale del servicio.
 */
@Component
public class RequestMapper {

    /** Crea la entidad a partir del JSON de entrada. El estado inicial lo decide el servicio. */
    public ServiceRequest toEntity(CreateRequestDTO dto) {
        ServiceRequest request = new ServiceRequest();
        request.setClientId(dto.getClientId());
        request.setServiceTypeId(dto.getServiceTypeId());
        request.setMode(dto.getMode());
        request.setDescription(clean(dto.getDescription()));
        request.setAddress(clean(dto.getAddress()));
        request.setLatitude(dto.getLatitude());
        request.setLongitude(dto.getLongitude());
        request.setVehiclePlate(plate(dto.getVehiclePlate()));
        request.setVehicleBrand(clean(dto.getVehicleBrand()));
        request.setVehicleType(clean(dto.getVehicleType()));
        return request;
    }

    /** Copia los campos editables (RF3.3) sobre la entidad existente, sin cambiar su identificador. */
    public void applyUpdate(ServiceRequest request, UpdateRequestDTO dto) {
        request.setDescription(clean(dto.getDescription()));
        request.setAddress(clean(dto.getAddress()));
        request.setLatitude(dto.getLatitude());
        request.setLongitude(dto.getLongitude());
        request.setVehiclePlate(plate(dto.getVehiclePlate()));
        request.setVehicleBrand(clean(dto.getVehicleBrand()));
        request.setVehicleType(clean(dto.getVehicleType()));
    }

    public RequestResponseDTO toResponse(ServiceRequest request) {
        RequestResponseDTO dto = new RequestResponseDTO();
        dto.setId(request.getId());
        dto.setClientId(request.getClientId());
        dto.setServiceTypeId(request.getServiceTypeId());
        dto.setProviderId(request.getProviderId());
        dto.setMode(request.getMode());
        dto.setStatus(request.getStatus());
        dto.setDescription(request.getDescription());
        dto.setAddress(request.getAddress());
        dto.setLatitude(request.getLatitude());
        dto.setLongitude(request.getLongitude());
        dto.setVehiclePlate(request.getVehiclePlate());
        dto.setVehicleBrand(request.getVehicleBrand());
        dto.setVehicleType(request.getVehicleType());
        dto.setCancellationReason(request.getCancellationReason());
        dto.setCancelledAt(toInstant(request.getCancelledAt()));
        dto.setCreatedAt(toInstant(request.getCreatedAt()));
        dto.setUpdatedAt(toInstant(request.getUpdatedAt()));
        return dto;
    }

    /** Quita espacios sobrantes y convierte el texto vacío en null. */
    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Las placas se guardan en mayúsculas. */
    private String plate(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toUpperCase(Locale.ROOT);
    }

    /** Las fechas se guardan en UTC; en la API viajan como Instant (ISO 8601 con Z). */
    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
