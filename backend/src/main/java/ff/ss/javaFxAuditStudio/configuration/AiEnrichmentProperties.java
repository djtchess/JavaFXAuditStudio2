package ff.ss.javaFxAuditStudio.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Proprietes de configuration de l'enrichissement IA (JAS-022 / JAS-017).
 *
 * <p>Active via {@link AiEnrichmentConfiguration} avec {@code @EnableConfigurationProperties}.
 * Chargee depuis le prefixe {@code ai.enrichment} dans {@code application.properties}.
 *
 * <p>Regles de securite :
 * - Les credentials ne doivent jamais apparaitre dans les logs.
 * - Les valeurs d'API key sont injectees uniquement via variables d'environnement.
 * - Si {@code enabled=true}, un provider valide et un credential present sont obligatoires.
 */
@ConfigurationProperties(prefix = "ai.enrichment")
public record AiEnrichmentProperties(
        boolean enabled,
        String provider,
        long timeoutMs,
        Credentials claudeCode,
        Credentials openai,
        boolean auditEnabled) {

    private static final long DEFAULT_TIMEOUT_MS = 10_000L;
    private static final String PROVIDER_CLAUDE = "claude-code";
    private static final String PROVIDER_OPENAI = "openai-gpt54";

    public record Credentials(String apiKey) {}

    /**
     * Retourne la cle API du fournisseur actif, ou null si absente.
     * Ne logge jamais la valeur retournee.
     */
    public String activeApiKey() {
        if (PROVIDER_CLAUDE.equals(provider) && claudeCode != null) {
            return claudeCode.apiKey();
        }
        if (PROVIDER_OPENAI.equals(provider) && openai != null) {
            return openai.apiKey();
        }
        return null;
    }

    /** Retourne vrai si le fournisseur configure est dans la liste des fournisseurs supportes. */
    public boolean isSupportedProvider() {
        return PROVIDER_CLAUDE.equals(provider) || PROVIDER_OPENAI.equals(provider);
    }

    /** Timeout effectif : la valeur configuree ou le defaut de 10 secondes. */
    public long effectiveTimeoutMs() {
        return timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
    }
}
