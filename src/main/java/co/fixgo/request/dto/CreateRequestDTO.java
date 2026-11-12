package co.fixgo.request.dto;

import co.fixgo.request.models.RequestMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos para crear una solicitud urgente (RF3.1) o programada (RF3.2).
 * Las reglas que dependen del modo (ubicación o dirección obligatoria) se validan en el servicio.
 */
public class CreateRequestDTO {

    // Temporal: cuando esté la seguridad (paso 3) se toma del token y deja de venir en el JSON
    @NotNull(message = "El cliente es obligatorio.")
    private UUID clientId;

    @NotNull(message = "El subservicio es obligatorio.")
    private UUID serviceTypeId;

    @NotNull(message = "El modo es obligatorio: URGENT o SCHEDULED.")
    private RequestMode mode;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 1000, message = "La descripción admite máximo 1000 caracteres.")
    private String description;

    @Size(max = 255, message = "La dirección admite máximo 255 caracteres.")
    private String address;

    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90.")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90.")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180.")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180.")
    private BigDecimal longitude;

    // Placa colombiana: carro ABC123 o moto ABC12D
    @Pattern(regexp = "^[A-Za-z]{3}[0-9]{2}[0-9A-Za-z]$",
            message = "La placa debe tener el formato ABC123 (carro) o ABC12D (moto).")
    private String vehiclePlate;

    @Size(max = 50, message = "La marca admite máximo 50 caracteres.")
    private String vehicleBrand;

    @Size(max = 30, message = "El tipo de vehículo admite máximo 30 caracteres.")
    private String vehicleType;

    public CreateRequestDTO() {
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public UUID getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(UUID serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    public RequestMode getMode() {
        return mode;
    }

    public void setMode(RequestMode mode) {
        this.mode = mode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public String getVehicleBrand() {
        return vehicleBrand;
    }

    public void setVehicleBrand(String vehicleBrand) {
        this.vehicleBrand = vehicleBrand;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }
}
