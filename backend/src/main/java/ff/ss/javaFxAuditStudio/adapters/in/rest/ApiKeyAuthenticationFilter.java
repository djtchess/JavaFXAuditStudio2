package ff.ss.javaFxAuditStudio.adapters.in.rest;

import java.io.IOException;

import ff.ss.javaFxAuditStudio.configuration.ApiSecurityEndpointCatalog;
import ff.ss.javaFxAuditStudio.configuration.ApiSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtre d'authentification minimal par bearer token applicatif.
 *
 * <p>Un fallback par query parameter est conserve pour les flux SSE browser
 * limites sur l'envoi d'en-tetes.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ApiSecurityProperties apiSecurityProperties;

    public ApiKeyAuthenticationFilter(final ApiSecurityProperties apiSecurityProperties) {
        this.apiSecurityProperties = apiSecurityProperties;
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        boolean shouldNotFilter = !apiSecurityProperties.apiKeyEnabled()
                || !ApiSecurityEndpointCatalog.requiresAuthentication(request.getRequestURI());
        return shouldNotFilter;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        boolean createdAuthentication = false;
        String presentedToken = resolvePresentedToken(request);

        if (apiSecurityProperties.matchesApiKey(presentedToken)) {
            createdAuthentication = authenticate(request);
        } else {
            LOG.debug("Acces protege refuse faute de bearer token valide pour {}", request.getRequestURI());
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (createdAuthentication) {
                SecurityContextHolder.clearContext();
            }
        }
    }

    private String resolvePresentedToken(final HttpServletRequest request) {
        String token = resolveBearerToken(request);

        if (token == null || token.isBlank()) {
            token = request.getParameter(apiSecurityProperties.effectiveTokenQueryParameter());
        }
        return token;
    }

    private String resolveBearerToken(final HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        String bearerToken = null;

        if (authorizationHeader != null
                && authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())
                && authorizationHeader.length() > BEARER_PREFIX.length()) {
            bearerToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        }
        return bearerToken;
    }

    private boolean authenticate(final HttpServletRequest request) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "internal-api-key",
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_INTERNAL_API"));

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        return true;
    }
}
