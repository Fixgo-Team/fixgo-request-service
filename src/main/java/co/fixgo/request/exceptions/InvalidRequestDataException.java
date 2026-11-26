package co.fixgo.request.exceptions;

import java.util.Map;

/**
 * Se lanza cuando un dato es inválido según el contexto y no se puede validar solo con anotaciones del DTO
 * (por ejemplo, una urgencia sin ubicación). El manejador global la convierte en HTTP 400, con el código
 * VALIDATION_ERROR o el de la regla que se incumple (ej. VEHICLE_DATA_REQUIRED).
 */
public class InvalidRequestDataException extends RuntimeException {

    public static final String DEFAULT_CODE = "VALIDATION_ERROR";

    private final String code;
    private final boolean customMessage;
    private final Map<String, String> details;

    /**
     * @param field   campo del JSON que falló
     * @param message motivo, en español
     */
    public InvalidRequestDataException(String field, String message) {
        super(message);
        this.code = DEFAULT_CODE;
        this.customMessage = false;
        this.details = Map.of(field, message);
    }

    /**
     * @param code    código técnico de la regla, ej. VEHICLE_DATA_REQUIRED
     * @param message mensaje para el usuario final, en español
     * @param details campo → motivo de cada dato que falló
     */
    public InvalidRequestDataException(String code, String message, Map<String, String> details) {
        super(message);
        this.code = code;
        this.customMessage = true;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    /** true si el mensaje describe toda la regla y debe mostrarse tal cual en vez del mensaje genérico. */
    public boolean hasCustomMessage() {
        return customMessage;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
