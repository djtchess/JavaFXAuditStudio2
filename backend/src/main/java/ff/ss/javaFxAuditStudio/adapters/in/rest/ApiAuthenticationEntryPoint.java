package ff.ss.javaFxAuditStudio.adapters.in.rest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reponse REST standardisee pour les refus d'authentification du socle minimal.
 */
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_MDC_KEY = "correlationId";

    @Override
    public void commence(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AuthenticationException authException) throws IOException {
        String correlationId = resolveCorrelationId(request);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(CORRELATION_HEADER, correlationId);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(buildBody(correlationId));
    }

    private String resolveCorrelationId(final HttpServletRequest request) {
        String correlationId = MDC.get(CORRELATION_MDC_KEY);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader(CORRELATION_HEADER);
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private String buildBody(final String correlationId) {
        return """
                {"status":401,"error":"Authentification requise","correlationId":"%s"}
                """.formatted(correlationId);
    }
}
