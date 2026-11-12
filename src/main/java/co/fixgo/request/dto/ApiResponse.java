package co.fixgo.request.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Contenedor universal (wrapper) de todas las respuestas de la API.
 * La raíz del JSON nunca cambia: success, data, error y meta.
 *
 * @param <T> tipo del recurso que viaja en data (un objeto o una lista)
 */
public class ApiResponse<T> {

    /** Versión de la API que se informa en meta. */
    public static final String API_VERSION = "v1.0";

    private final boolean success; // true si la operación terminó con éxito
    private final T data;          // recurso solicitado; null si hubo error
    private final ApiError error;  // detalle del fallo; null si todo salió bien
    private final ApiMeta meta;    // datos de contexto: fecha y versión

    /** Respuesta EXITOSA: success = true y error = null. */
    public ApiResponse(T data) {
        this.success = true;
        this.data = data;
        this.error = null;
        this.meta = new ApiMeta();
    }

    /** Respuesta FALLIDA: success = false y data = null. */
    public ApiResponse(String code, String message, Map<String, String> details) {
        this.success = false;
        this.data = null;
        this.error = new ApiError(code, message, details);
        this.meta = new ApiMeta();
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }

    public ApiMeta getMeta() {
        return meta;
    }

    /** Detalle del error. */
    public static class ApiError {

        private final String code;                 // código técnico para el programador, ej. VALIDATION_ERROR
        private final String message;              // mensaje para el usuario final, en español
        private final Map<String, String> details; // campo → motivo; null si no aplica

        public ApiError(String code, String message, Map<String, String> details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, String> getDetails() {
            return details;
        }
    }

    /** Datos de contexto de la respuesta. */
    public static class ApiMeta {

        private final String timestamp = Instant.now().toString(); // ISO 8601 en UTC
        private final String apiVersion = API_VERSION;

        public String getTimestamp() {
            return timestamp;
        }

        public String getApiVersion() {
            return apiVersion;
        }
    }
}
