package ff.ss.javaFxAuditStudio.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Proprietes de configuration du pipeline de sanitisation (JAS-018).
 *
 * <p>Liees au prefixe {@code ai.sanitization} dans {@code application.properties}.
 * Enregistrees via {@code @EnableConfigurationProperties} dans
 * {@code AiEnrichmentOrchestraConfiguration}.
 *
 * @param profileVersion Version du profil de sanitisation (defaut : "1.0")
 * @param maxTokens      Plafond de tokens estimes acceptes (defaut : 4000)
 * @param enabled        Desensibilisation active (defaut : true)
 */
@ConfigurationProperties(prefix = "ai.sanitization")
public record SanitizationProperties(
        String profileVersion,
        int maxTokens,
        boolean enabled) {

    public SanitizationProperties {
        profileVersion = (profileVersion != null && !profileVersion.isBlank())
                ? profileVersion : "1.0";
        maxTokens = maxTokens > 0 ? maxTokens : 4000;
    }
}
