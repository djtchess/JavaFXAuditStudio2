package ff.ss.javaFxAuditStudio.configuration;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration de l'enrichissement IA.
 *
 * <p>Valide les proprietes au demarrage de l'application :
 * - Si l'enrichissement est desactive, log informatif et fin.
 * - Si l'enrichissement est active, verifie le provider et la presence du credential.
 *   En cas de configuration invalide, l'application refuse de demarrer.
 *
 * <p>JAS-022 : les credentials ne sont jamais loggues, meme au niveau DEBUG.
 */
@Configuration
@EnableConfigurationProperties(AiEnrichmentProperties.class)
public class AiEnrichmentConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(AiEnrichmentConfiguration.class);

    private final AiEnrichmentProperties properties;

    public AiEnrichmentConfiguration(final AiEnrichmentProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (!properties.enabled()) {
            LOG.info("Enrichissement IA desactive (ai.enrichment.enabled=false)");
            return;
        }
        validateProvider();
        validateCredential();
        LOG.info("Enrichissement IA active — fournisseur : {}, timeout : {}ms",
                properties.provider(), properties.effectiveTimeoutMs());
        LOG.debug("Credential present pour {} (valeur masquee)", properties.provider());
    }

    private void validateProvider() {
        String provider = properties.provider();
        if (provider == null || provider.isBlank() || !properties.isSupportedProvider()) {
            throw new IllegalStateException(
                "ai.enrichment.provider doit etre 'claude-code', 'openai-gpt54' ou 'claude-code-cli' — valeur recue : "
                + (provider == null ? "null" : "'" + provider + "'"));
        }
    }

    private void validateCredential() {
        if (!properties.isCredentialRequired()) {
            LOG.info("Fournisseur CLI — aucun credential API requis (authentification CLI locale)");
            return;
        }
        String apiKey = properties.activeApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "Credential manquant pour le fournisseur " + properties.provider()
                + " : configurer la variable d'environnement appropriee "
                + "(CLAUDE_API_KEY ou OPENAI_API_KEY)");
        }
    }
}
