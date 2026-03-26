package ff.ss.javaFxAuditStudio.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration optionnelle du proxy HTTP pour les appels LLM sortants.
 *
 * <p>Active via le prefixe {@code ai.enrichment.proxy}.
 * Si {@code host} est null ou vide, aucun proxy n'est configure (connexion directe).
 *
 * <pre>
 * # application.properties
 * ai.enrichment.proxy.host=proxy.corporate.com
 * ai.enrichment.proxy.port=3128
 * </pre>
 */
@ConfigurationProperties(prefix = "ai.enrichment.proxy")
public record AiHttpProxyProperties(String host, int port) {

    /** Retourne vrai si un proxy est configure (host non vide). */
    public boolean isConfigured() {
        return host != null && !host.isBlank();
    }

    /** Port par defaut si non specifie : 3128 (Squid/standard). */
    @Override
    public int port() {
        return port > 0 ? port : 3128;
    }
}
