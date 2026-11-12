package co.fixgo.request.exceptions;

import co.fixgo.request.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Escudo global de errores: convierte cada excepción en un {@link ApiResponse} con success = false
 * y un código HTTP que coincide con el error. Los controladores no usan try-catch.
 * Los códigos 401 y 403 los agrega la configuración de seguridad.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 400: el cuerpo no cumple las validaciones del DTO (@Valid). Lista cada campo que falló. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidBody(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Los datos enviados son inválidos.", details);
    }

    /** 400: un dato es inválido según el contexto (validación hecha en el servicio). */
    @ExceptionHandler(InvalidRequestDataException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidData(InvalidRequestDataException ex) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Los datos enviados son inválidos.", ex.getDetails());
    }

    /** 400: parámetros de ruta o de consulta que no cumplen sus validaciones. */
    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidParameters(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Los parámetros enviados son inválidos.", null);
    }

    /** 400: JSON mal formado, tipo de dato incorrecto o parámetro obligatorio ausente. */
    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "La solicitud está mal formada o tiene un tipo de dato incorrecto.", null);
    }

    /** 404: el recurso de negocio no existe. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), null);
    }

    /** 404: la ruta no existe. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoRoute(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND", "La ruta solicitada no existe.", null);
    }

    /** 405: método HTTP no soportado por la ruta. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "El método HTTP no está permitido en esta ruta.", null);
    }

    /** 409: se incumple una regla de negocio o una transición de estado. */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRule(BusinessRuleException ex) {
        return build(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), null);
    }

    /** 500: error no previsto. El detalle queda solo en el log; nunca se envía el stack trace al cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrió un error inesperado. Intenta de nuevo más tarde.", null);
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String code, String message,
                                                    Map<String, String> details) {
        return ResponseEntity.status(status).body(new ApiResponse<>(code, message, details));
    }
}
