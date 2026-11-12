package co.fixgo.request.models;

/**
 * Estados de una solicitud (Anexo A.3 del SRS v3.0).
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

    /** RF3.4: solo se cancela antes de que el servicio esté en curso. */
    public boolean isCancellable() {
        return this == PENDING || this == OPEN || this == ACCEPTED || this == ON_THE_WAY;
    }
}
