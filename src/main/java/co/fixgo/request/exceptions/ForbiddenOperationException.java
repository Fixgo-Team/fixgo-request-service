package co.fixgo.request.exceptions;

/**
 * Se lanza cuando quien llama no tiene permiso sobre el recurso o la operación (rol o propiedad incorrectos).
 * El manejador global la convierte en HTTP 403 Forbidden.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
