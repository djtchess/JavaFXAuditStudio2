package ff.ss.javaFxAuditStudio.adapters.in.rest;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CorrelationFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";
    private static final String SESSION_MDC_KEY = "sessionId";
    private static final Pattern SESSION_PATH_PATTERN = Pattern.compile("/(?:analysis/sessions|analyses)/([^/]+)");

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request);
        String sessionId = resolveSessionId(request);
        try {
            MDC.put(MDC_KEY, correlationId);
            populateSessionId(sessionId);
            response.setHeader(HEADER_NAME, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
            MDC.remove(SESSION_MDC_KEY);
        }
    }

    private String resolveCorrelationId(final HttpServletRequest request) {
        String header = request.getHeader(HEADER_NAME);
        if (header != null && !header.isBlank()) {
            return header;
        }
        return UUID.randomUUID().toString();
    }

    private void populateSessionId(final String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            MDC.put(SESSION_MDC_KEY, sessionId);
        }
    }

    private String resolveSessionId(final HttpServletRequest request) {
        Matcher matcher = SESSION_PATH_PATTERN.matcher(request.getRequestURI());
        String sessionId;

        sessionId = null;
        if (matcher.find()) {
            sessionId = matcher.group(1);
        }
        return sessionId;
    }
}
