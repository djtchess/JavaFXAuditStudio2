package ff.ss.javaFxAuditStudio.configuration;

import org.springframework.util.AntPathMatcher;

/**
 * Catalogue des endpoints sensibles couverts par le socle de securite minimal.
 */
public final class ApiSecurityEndpointCatalog {

    public static final String[] PUBLIC_ENDPOINT_PATTERNS = {
        "/error",
        "/actuator/health",
        "/actuator/health/**",
        "/actuator/info"
    };

    public static final String[] PROTECTED_ENDPOINT_PATTERNS = {
        "/actuator/**",
        "/api-docs",
        "/api-docs/**",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/api/v1/ai-enrichment/**",
        "/api/v1/ai/reference-patterns/**",
        "/api/v1/projects/analysis/**",
        "/api/v1/analysis/sessions/*/llm-audit",
        "/api/v1/analysis/sessions/*/enrich",
        "/api/v1/analysis/sessions/*/review",
        "/api/v1/analysis/sessions/*/coherence",
        "/api/v1/analysis/sessions/*/refine",
        "/api/v1/analysis/sessions/*/preview-sanitized",
        "/api/v1/analysis/sessions/*/generate/ai",
        "/api/v1/analysis/sessions/*/generate/ai/**",
        "/api/v1/analysis/sessions/*/artifacts/ai",
        "/api/v1/analysis/sessions/*/artifacts/ai/**",
        "/api/v1/analysis/sessions/*/rules/*/classification",
        "/api/v1/analysis/sessions/*/rules/*/classification/history",
        "/api/v1/analyses/*/enrich",
        "/api/v1/analyses/*/review",
        "/api/v1/analyses/*/coherence",
        "/api/v1/analyses/*/refine",
        "/api/v1/analyses/*/preview-sanitized",
        "/api/v1/analyses/*/generate/ai",
        "/api/v1/analyses/*/generate/ai/**",
        "/api/v1/analyses/*/artifacts/ai",
        "/api/v1/analyses/*/artifacts/ai/**",
        "/api/v1/analyses/*/rules/*/classification",
        "/api/v1/analyses/*/rules/*/classification/history"
    };

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private ApiSecurityEndpointCatalog() {
    }

    public static boolean requiresAuthentication(final String requestUri) {
        boolean requiresAuthentication = matchesAny(PROTECTED_ENDPOINT_PATTERNS, requestUri)
                && !matchesAny(PUBLIC_ENDPOINT_PATTERNS, requestUri);
        return requiresAuthentication;
    }

    private static boolean matchesAny(final String[] patterns, final String requestUri) {
        boolean matchesAny = false;

        for (String pattern : patterns) {
            if (PATH_MATCHER.match(pattern, requestUri)) {
                matchesAny = true;
                break;
            }
        }
        return matchesAny;
    }
}
