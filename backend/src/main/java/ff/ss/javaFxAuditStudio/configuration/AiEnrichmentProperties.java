package ff.ss.javaFxAuditStudio.configuration;

import java.util.Map;

import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
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
        boolean auditEnabled,
        String cliCommand,
        Retry retry,
        Map<TaskType, Integer> maxTokensByTask,
        Integer maxResponseSizeBytes,
        Integer maxDegradationReasonLength) {

    private static final long DEFAULT_TIMEOUT_MS = 10_000L;
    private static final String PROVIDER_CLAUDE = "claude-code";
    private static final String PROVIDER_OPENAI = "openai-gpt54";
    private static final String PROVIDER_CLAUDE_CLI = "claude-code-cli";
    private static final int DEFAULT_MAX_TOKENS_STANDARD = 1_024;
    private static final int DEFAULT_MAX_TOKENS_ARTIFACT_REVIEW = 2_048;
    private static final int DEFAULT_MAX_TOKENS_SPRING_GENERATION = 4_096;
    private static final int DEFAULT_MAX_RESPONSE_SIZE_BYTES = 512 * 1024;
    private static final int DEFAULT_MAX_DEGRADATION_REASON_LENGTH = 200;

    public record Credentials(String apiKey) {}

    /**
     * Configuration du retry avec backoff exponentiel (IAP-4).
     *
     * <p>Toutes les valeurs sont nullables : les methodes {@code effective*} appliquent
     * les valeurs par defaut si la configuration est absente ou invalide.
     */
    public record Retry(Integer maxRetries, Long initialBackoffMs, Double multiplier) {

        public int effectiveMaxRetries() {
            return (maxRetries != null && maxRetries >= 0) ? maxRetries : 2;
        }

        public long effectiveInitialBackoffMs() {
            return (initialBackoffMs != null && initialBackoffMs > 0) ? initialBackoffMs : 500L;
        }

        public double effectiveMultiplier() {
            return (multiplier != null && multiplier > 1.0) ? multiplier : 2.0;
        }
    }

    /** Retourne la configuration de retry effective, jamais nulle. */
    public Retry effectiveRetry() {
        return retry != null ? retry : new Retry(null, null, null);
    }

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
        return PROVIDER_CLAUDE.equals(provider) || PROVIDER_OPENAI.equals(provider)
                || PROVIDER_CLAUDE_CLI.equals(provider);
    }

    /**
     * Retourne vrai si ce fournisseur necessite une cle API.
     * Le fournisseur CLI utilise l'authentification du CLI local, pas de cle API.
     */
    public boolean isCredentialRequired() {
        return PROVIDER_CLAUDE.equals(provider) || PROVIDER_OPENAI.equals(provider);
    }

    /** Timeout effectif : la valeur configuree ou le defaut de 10 secondes. */
    public long effectiveTimeoutMs() {
        return timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
    }

    /** Commande CLI effective : valeur configuree ou "claude" par defaut. */
    public String effectiveCliCommand() {
        return (cliCommand != null && !cliCommand.isBlank()) ? cliCommand : "claude";
    }

    /** Max tokens effectif pour une tache donnee, avec fallback par defaut. */
    public int effectiveMaxTokens(final TaskType taskType) {
        if (taskType == null) {
            return DEFAULT_MAX_TOKENS_STANDARD;
        }
        if (maxTokensByTask != null) {
            Integer configuredValue = maxTokensByTask.get(taskType);
            if (configuredValue != null && configuredValue > 0) {
                return configuredValue;
            }
        }
        return defaultMaxTokens(taskType);
    }

    /** Retourne le fournisseur configure en tant qu'enum typesafe. */
    public LlmProvider providerEnum() {
        return LlmProvider.fromString(provider);
    }

    /** Taille maximale effective d'une reponse LLM acceptee avant truncation defensive. */
    public int effectiveMaxResponseSizeBytes() {
        return (maxResponseSizeBytes != null && maxResponseSizeBytes > 0)
                ? maxResponseSizeBytes
                : DEFAULT_MAX_RESPONSE_SIZE_BYTES;
    }

    /** Longueur maximale effective d'un message de degradation avant troncature defensive. */
    public int effectiveMaxDegradationReasonLength() {
        return (maxDegradationReasonLength != null && maxDegradationReasonLength > 0)
                ? maxDegradationReasonLength
                : DEFAULT_MAX_DEGRADATION_REASON_LENGTH;
    }

    private static int defaultMaxTokens(final TaskType taskType) {
        return switch (taskType) {
            case ARTIFACT_REVIEW -> DEFAULT_MAX_TOKENS_ARTIFACT_REVIEW;
            case SPRING_BOOT_GENERATION -> DEFAULT_MAX_TOKENS_SPRING_GENERATION;
            case NAMING, DESCRIPTION, CLASSIFICATION_HINT -> DEFAULT_MAX_TOKENS_STANDARD;
        };
    }
}
