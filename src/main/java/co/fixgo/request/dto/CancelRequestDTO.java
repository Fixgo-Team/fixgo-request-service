package co.fixgo.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Motivo para cancelar una solicitud (RF3.4: el motivo es obligatorio).
 */
public class CancelRequestDTO {

    @NotBlank(message = "El motivo de la cancelación es obligatorio.")
    @Size(max = 500, message = "El motivo admite máximo 500 caracteres.")
    private String reason;

    public CancelRequestDTO() {
    }

    public CancelRequestDTO(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
