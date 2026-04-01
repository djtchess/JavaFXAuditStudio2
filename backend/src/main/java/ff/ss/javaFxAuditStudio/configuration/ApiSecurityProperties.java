package ff.ss.javaFxAuditStudio.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Proprietes du socle securite minimal backend.
 *
 * <p>Le mode "enabled" reste opt-in pour ne pas casser les parcours frontend
 * tant que le bearer token n'est pas propage par tous les clients.
 */
@ConfigurationProperties(prefix = "app.security")
public class ApiSecurityProperties {

    private boolean apiKeyEnabled;
    private String apiKey = "";
    private String tokenQueryParameter = "apiKey";

    public boolean apiKeyEnabled() {
        return apiKeyEnabled;
    }

    public void setApiKeyEnabled(final boolean apiKeyEnabled) {
        this.apiKeyEnabled = apiKeyEnabled;
    }

    public String apiKey() {
        return apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public String effectiveTokenQueryParameter() {
        String queryParameter = tokenQueryParameter;

        if (queryParameter == null || queryParameter.isBlank()) {
            queryParameter = "apiKey";
        }
        return queryParameter;
    }

    public void setTokenQueryParameter(final String tokenQueryParameter) {
        this.tokenQueryParameter = tokenQueryParameter;
    }

    public boolean hasApiKeyConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public boolean matchesApiKey(final String candidate) {
        return hasApiKeyConfigured() && apiKey.equals(candidate);
    }
}
