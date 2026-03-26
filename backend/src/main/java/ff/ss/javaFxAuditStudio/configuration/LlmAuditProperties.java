package ff.ss.javaFxAuditStudio.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Proprietes de configuration de l'audit LLM (JAS-029).
 *
 * <p>Prefixe : {@code ai.audit}
 * Exemple : {@code ai.audit.enabled=true}
 */
@ConfigurationProperties(prefix = "ai.audit")
public record LlmAuditProperties(boolean enabled) {

    /**
     * Constructeur compact avec valeur par defaut.
     * enabled=true par defaut si absent.
     */
    public LlmAuditProperties() {
        this(true);
    }
}
