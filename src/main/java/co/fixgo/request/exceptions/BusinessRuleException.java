package co.fixgo.request.exceptions;

/**
 * Se lanza cuando una operación incumple una regla de negocio (RN) o una transición de estado no permitida.
 * El manejador global la convierte en HTTP 409 Conflict.
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    /** Usa el código genérico BUSINESS_RULE_VIOLATION. */
    public BusinessRuleException(String message) {
        this("BUSINESS_RULE_VIOLATION", message);
    }

    /**
     * @param code    código técnico de la regla, ej. QUOTE_ALREADY_ACCEPTED (RN-04)
     * @param message mensaje para el usuario final, en español
     */
    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
