package ff.ss.javaFxAuditStudio.configuration;

import java.util.Map;

import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

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
        String cliModel,
        Retry retry,
        Map<TaskType, Integer> maxTokensByTask,
        Integer maxResponseSizeBytes,
        Integer maxDegradationReasonLength,
        Map<TaskType, PromptContextBudget> promptContextBudgetByTask) {

    private static final long DEFAULT_TIMEOUT_MS = 10_000L;
    private static final String DEFAULT_CLAUDE_CLI_COMMAND = "claude";
    private static final String DEFAULT_OPENAI_CODEX_CLI_COMMAND = "codex";
    private static final String DEFAULT_OPENAI_CODEX_CLI_MODEL = "gpt-5.3-codex";
    private static final int DEFAULT_MAX_TOKENS_STANDARD = 1_024;
    private static final int DEFAULT_MAX_TOKENS_ARTIFACT_REVIEW = 2_048;
    private static final int DEFAULT_MAX_TOKENS_ARTIFACT_COHERENCE = 3_072;
    private static final int DEFAULT_MAX_TOKENS_SPRING_GENERATION = 4_096;
    private static final int DEFAULT_MAX_RESPONSE_SIZE_BYTES = 512 * 1024;
    private static final int DEFAULT_MAX_DEGRADATION_REASON_LENGTH = 200;
    private static final PromptContextBudget DEFAULT_PROMPT_CONTEXT_BUDGET_NAMING =
            new PromptContextBudget(4_000, 1_000, 2_000, 2, 2_000, 2);
    private static final PromptContextBudget DEFAULT_PROMPT_CONTEXT_BUDGET_DESCRIPTION =
            new PromptContextBudget(4_000, 1_000, 2_000, 2, 2_000, 2);
    private static final PromptContextBudget DEFAULT_PROMPT_CONTEXT_BUDGET_CLASSIFICATION_HINT =
            new PromptContextBudget(4_000, 1_000, 2_000, 2, 2_000, 2);
    private static final PromptContextBudget DEFAULT_PROMPT_CONTEXT_BUDGET_ARTIFACT_REVIEW =
            new PromptContextBudget(6_000, 1_500, 8_000, 4, 6_000, 4);
    private static final PromptContextBudget DEFAULT_PROMPT_CONTEXT_BUDGET_ARTIFACT_REFINEMENT =
            new PromptContextBudget(10_000, 2_000, 8_000, 3, 6_000, 4);
    private static final PromptContextBudget DEFAULT_PROMPT_CONTEXT_BUDGET_ARTIFACT_COHERENCE =
            new PromptContextBudget(8_000, 1_500, 12_000, 5, 8_000, 5);
    private static final PromptContextBudget DEFAULT_PROMPT_CONTEXT_BUDGET_SPRING_BOOT_GENERATION =
            new PromptContextBudget(12_000, 1_500, 8_000, 3, 10_000, 5);

    public record Credentials(String apiKey) {}

    @ConstructorBinding
    public AiEnrichmentProperties {
    }

    public AiEnrichmentProperties(
            final boolean enabled,
            final String provider,
            final long timeoutMs,
            final Credentials claudeCode,
            final Credentials openai,
            final boolean auditEnabled,
            final String cliCommand,
            final Retry retry,
            final Map<TaskType, Integer> maxTokensByTask,
            final Integer maxResponseSizeBytes,
            final Integer maxDegradationReasonLength,
            final Map<TaskType, PromptContextBudget> promptContextBudgetByTask) {
        this(
                enabled,
                provider,
                timeoutMs,
                claudeCode,
                openai,
                auditEnabled,
                cliCommand,
                null,
                retry,
                maxTokensByTask,
                maxResponseSizeBytes,
                maxDegradationReasonLength,
                promptContextBudgetByTask);
    }

    public record PromptContextBudget(
            Integer maxCodeFragmentChars,
            Integer maxInstructionChars,
            Integer maxArtifactDetailsChars,
            Integer maxArtifactDetailsItems,
            Integer maxReferencePatternsChars,
            Integer maxReferencePatternsItems) {

        public PromptContextBudget merge(final PromptContextBudget fallback) {
            PromptContextBudget effectiveFallback = java.util.Objects.requireNonNull(
                    fallback, "fallback must not be null");
            return new PromptContextBudget(
                    positiveOrFallback(maxCodeFragmentChars, effectiveFallback.maxCodeFragmentChars),
                    positiveOrFallback(maxInstructionChars, effectiveFallback.maxInstructionChars),
                    positiveOrFallback(maxArtifactDetailsChars, effectiveFallback.maxArtifactDetailsChars),
                    positiveOrFallback(maxArtifactDetailsItems, effectiveFallback.maxArtifactDetailsItems),
                    positiveOrFallback(maxReferencePatternsChars, effectiveFallback.maxReferencePatternsChars),
                    positiveOrFallback(maxReferencePatternsItems, effectiveFallback.maxReferencePatternsItems));
        }

        private static Integer positiveOrFallback(final Integer candidate, final Integer fallback) {
            return candidate != null && candidate > 0 ? candidate : fallback;
        }
    }

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
        return apiKeyFor(providerEnum());
    }

    public String apiKeyFor(final LlmProvider targetProvider) {
        if (targetProvider == LlmProvider.CLAUDE_CODE && claudeCode != null) {
            return claudeCode.apiKey();
        }
        if (targetProvider == LlmProvider.OPENAI_GPT54 && openai != null) {
            return openai.apiKey();
        }
        return null;
    }

    /** Retourne vrai si le fournisseur configure est dans la liste des fournisseurs supportes. */
    public boolean isSupportedProvider() {
        return providerEnum() != LlmProvider.NONE;
    }

    /**
     * Retourne vrai si ce fournisseur necessite une cle API.
     * Le fournisseur CLI utilise l'authentification du CLI local, pas de cle API.
     */
    public boolean isCredentialRequired() {
        LlmProvider activeProvider = providerEnum();
        return activeProvider == LlmProvider.CLAUDE_CODE
                || activeProvider == LlmProvider.OPENAI_GPT54;
    }

    /** Timeout effectif : la valeur configuree ou le defaut de 10 secondes. */
    public long effectiveTimeoutMs() {
        return timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
    }

    /** Commande CLI effective avec defaut adapte au fournisseur CLI configure. */
    public String effectiveCliCommand() {
        return effectiveCliCommand(providerEnum());
    }

    /** Commande CLI effective pour un provider donne. */
    public String effectiveCliCommand(final LlmProvider targetProvider) {
        if (cliCommand != null && !cliCommand.isBlank()) {
            return cliCommand;
        }
        return targetProvider == LlmProvider.OPENAI_CODEX_CLI
                ? DEFAULT_OPENAI_CODEX_CLI_COMMAND
                : DEFAULT_CLAUDE_CLI_COMMAND;
    }

    /** Modele CLI effectif pour OpenAI Codex CLI. Retourne null pour les autres fournisseurs. */
    public String effectiveCliModel() {
        return effectiveCliModel(providerEnum());
    }

    /** Modele CLI effectif pour un provider donne. */
    public String effectiveCliModel(final LlmProvider targetProvider) {
        if (cliModel != null && !cliModel.isBlank()) {
            return cliModel;
        }
        return targetProvider == LlmProvider.OPENAI_CODEX_CLI
                ? DEFAULT_OPENAI_CODEX_CLI_MODEL
                : null;
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

    /**
     * Retourne le budget de contexte promptable effectif pour un type de tache.
     *
     * <p>La configuration peut surcharger partiellement le budget. Les champs absents ou
     * invalides retombent sur un profil par defaut specifique au {@link TaskType}.
     */
    public PromptContextBudget effectivePromptContextBudget(final TaskType taskType) {
        PromptContextBudget fallback = defaultPromptContextBudget(taskType);
        if (promptContextBudgetByTask == null || promptContextBudgetByTask.isEmpty()) {
            return fallback;
        }
        PromptContextBudget configured = promptContextBudgetByTask.get(normalizeTaskType(taskType));
        return configured != null ? configured.merge(fallback) : fallback;
    }

    private static int defaultMaxTokens(final TaskType taskType) {
        return switch (taskType) {
            case ARTIFACT_REVIEW -> DEFAULT_MAX_TOKENS_ARTIFACT_REVIEW;
            case ARTIFACT_COHERENCE -> DEFAULT_MAX_TOKENS_ARTIFACT_COHERENCE;
            case ARTIFACT_REFINEMENT, SPRING_BOOT_GENERATION -> DEFAULT_MAX_TOKENS_SPRING_GENERATION;
            case NAMING, DESCRIPTION, CLASSIFICATION_HINT -> DEFAULT_MAX_TOKENS_STANDARD;
        };
    }

    private static TaskType normalizeTaskType(final TaskType taskType) {
        return taskType != null ? taskType : TaskType.CLASSIFICATION_HINT;
    }

    private static PromptContextBudget defaultPromptContextBudget(final TaskType taskType) {
        return switch (normalizeTaskType(taskType)) {
            case NAMING -> DEFAULT_PROMPT_CONTEXT_BUDGET_NAMING;
            case DESCRIPTION -> DEFAULT_PROMPT_CONTEXT_BUDGET_DESCRIPTION;
            case CLASSIFICATION_HINT -> DEFAULT_PROMPT_CONTEXT_BUDGET_CLASSIFICATION_HINT;
            case ARTIFACT_REVIEW -> DEFAULT_PROMPT_CONTEXT_BUDGET_ARTIFACT_REVIEW;
            case ARTIFACT_REFINEMENT -> DEFAULT_PROMPT_CONTEXT_BUDGET_ARTIFACT_REFINEMENT;
            case ARTIFACT_COHERENCE -> DEFAULT_PROMPT_CONTEXT_BUDGET_ARTIFACT_COHERENCE;
            case SPRING_BOOT_GENERATION -> DEFAULT_PROMPT_CONTEXT_BUDGET_SPRING_BOOT_GENERATION;
        };
    }
}
