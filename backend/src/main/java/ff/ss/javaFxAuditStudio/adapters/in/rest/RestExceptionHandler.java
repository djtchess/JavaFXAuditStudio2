package ff.ss.javaFxAuditStudio.adapters.in.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RestExceptionHandler.class);
    private static final String MDC_KEY = "correlationId";
    private static final String ERROR_MESSAGE = "Erreur interne";
    private static final String INVALID_REQUEST_MESSAGE = "Requête invalide";

    record ErrorResponse(int status, String error, String correlationId) {
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException exception) {
        String correlationId = resolveCorrelationId();
        LOG.warn("Invalid request [correlationId={}, message={}]", correlationId, exception.getMessage());
        ErrorResponse body = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), INVALID_REQUEST_MESSAGE, correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(final IllegalStateException exception) {
        String correlationId = resolveCorrelationId();
        LOG.warn("Conflict [correlationId={}, message={}]", correlationId, exception.getMessage());
        ErrorResponse body = new ErrorResponse(HttpStatus.CONFLICT.value(), exception.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(final Exception exception) {
        String correlationId = resolveCorrelationId();
        LOG.error("Unhandled exception [correlationId={}, exceptionClass={}]",
                correlationId, exception.getClass().getName());
        ErrorResponse body = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_MESSAGE, correlationId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String resolveCorrelationId() {
        String value = MDC.get(MDC_KEY);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return "unavailable";
    }
}
