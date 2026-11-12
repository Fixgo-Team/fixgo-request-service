package co.fixgo.request.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Datos editables de una solicitud antes de que sea aceptada (RF3.3).
 * El cliente, el subservicio y el modo no cambian: si cambian, es otra solicitud.
 */
public class UpdateRequestDTO {

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

    @Pattern(regexp = "^[A-Za-z]{3}[0-9]{2}[0-9A-Za-z]$",
            message = "La placa debe tener el formato ABC123 (carro) o ABC12D (moto).")
    private String vehiclePlate;

    @Size(max = 50, message = "La marca admite máximo 50 caracteres.")
    private String vehicleBrand;

    @Size(max = 30, message = "El tipo de vehículo admite máximo 30 caracteres.")
    private String vehicleType;

    public UpdateRequestDTO() {
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
