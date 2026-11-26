package co.fixgo.request.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Franja horaria que el cliente ofrece en una solicitud programada (RF3.2). Fechas en ISO 8601 y UTC.
 */
public class DateSlotDTO {

    @NotNull(message = "La hora de inicio de la franja es obligatoria.")
    private Instant startsAt;

    @NotNull(message = "La hora de fin de la franja es obligatoria.")
    private Instant endsAt;

    public DateSlotDTO() {
    }

    public DateSlotDTO(Instant startsAt, Instant endsAt) {
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }
}
