package co.fixgo.request.exceptions;

/**
 * Se lanza cuando otro servicio del que depende la operación no responde a tiempo o falla (RNF-12).
 * El manejador global la convierte en HTTP 503 DEPENDENCY_UNAVAILABLE. Como es una RuntimeException,
 * deshace la transacción en curso: no queda ninguna solicitud a medias.
 */
public class DependencyUnavailableException extends RuntimeException {

    private final String dependency;

    /**
     * @param dependency nombre del servicio que falló, ej. dispatch-matchmaking (solo para el log)
     * @param message    mensaje para el usuario final, en español
     */
    public DependencyUnavailableException(String dependency, String message) {
        super(message);
        this.dependency = dependency;
    }

    public DependencyUnavailableException(String dependency, String message, Throwable cause) {
        super(message, cause);
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}
