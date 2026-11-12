package co.fixgo.request.exceptions;

/**
 * Se lanza cuando el recurso pedido no existe. El manejador global la convierte en HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
