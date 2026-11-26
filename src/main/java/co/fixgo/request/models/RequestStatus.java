package co.fixgo.request.models;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de una solicitud (Anexo A.4 del SRS v3.1).
 */
public enum RequestStatus {

    /** Urgencia recién creada, esperando que un profesional la acepte. */
    PENDING,

    /** Programada publicada, recibiendo cotizaciones. */
    OPEN,

    /** Profesional asignado. */
    ACCEPTED,

    /** El profesional va en camino. */
    ON_THE_WAY,

    /** El servicio está en curso. */
    IN_PROGRESS,

    /** Completada con informe de servicio (RN-05). */
    COMPLETED,

    /** Cancelada con motivo (RF3.4). */
    CANCELLED,

    /** Programada sin cotización aceptada a los 7 días (RF3.9). */
    EXPIRED;

    /** RF3.3: solo se modifica antes de que sea aceptada. */
    public boolean isEditable() {
        return this == PENDING || this == OPEN;
    }

    /**
     * Estados a los que puede pasar desde este (Anexo A.4, tabla 26; data-dictionary.md §1). Los finales no tienen
     * destinos. La reapertura por garantía (COMPLETED → ACCEPTED, RF4.9) no se modela: está bloqueada (Q-033).
     */
    public Set<RequestStatus> allowedTargets() {
        return switch (this) {
            case PENDING -> EnumSet.of(ACCEPTED, OPEN, CANCELLED);
            case OPEN -> EnumSet.of(ACCEPTED, EXPIRED, CANCELLED);
            case ACCEPTED -> EnumSet.of(ON_THE_WAY, CANCELLED);
            case ON_THE_WAY -> EnumSet.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS -> EnumSet.of(COMPLETED, CANCELLED);
            case COMPLETED, CANCELLED, EXPIRED -> EnumSet.noneOf(RequestStatus.class);
        };
    }

    public boolean canTransitionTo(RequestStatus target) {
        return allowedTargets().contains(target);
    }

    /** Un estado final no admite más cambios. */
    public boolean isFinal() {
        return allowedTargets().isEmpty();
    }
}