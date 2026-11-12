package co.fixgo.request.exceptions;

import java.util.Map;

/**
 * Se lanza cuando un dato es inválido según el contexto y no se puede validar solo con anotaciones del DTO
 * (por ejemplo, una urgencia sin ubicación). El manejador global la convierte en HTTP 400 VALIDATION_ERROR.
 */
public class InvalidRequestDataException extends RuntimeException {

    private final Map<String, String> details;

    /**
     * @param field   campo del JSON que falló
     * @param message motivo, en español
     */
    public InvalidRequestDataException(String field, String message) {
        super(message);
        this.details = Map.of(field, message);
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
