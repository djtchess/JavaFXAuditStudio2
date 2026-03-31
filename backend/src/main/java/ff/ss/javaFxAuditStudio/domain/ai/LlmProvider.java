package ff.ss.javaFxAuditStudio.domain.ai;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Fournisseur LLM utilisé pour l'enrichissement IA (IAP-2).
 *
 * <p>Remplace les constantes String ("claude-code", "openai-gpt54"...) dispersées
 * dans les adapters et la configuration par un enum typesafe du domaine.
 *
 * <p>L'annotation {@link JsonValue} sur {@link #value()} garantit que la
 * sérialisation JSON produit la valeur métier ("claude-code") et non le nom
 * de constante ("CLAUDE_CODE"), préservant le contrat API frontend.
 */
public enum LlmProvider {

    /** API Anthropic Claude (REST HTTP). */
    CLAUDE_CODE("claude-code"),

    /** API OpenAI GPT-4o (REST HTTP). */
    OPENAI_GPT54("openai-gpt54"),

    /** Claude Code CLI local (ProcessBuilder). */
    CLAUDE_CODE_CLI("claude-code-cli"),

    /** OpenAI Codex CLI local (abonnement ChatGPT/Codex). */
    OPENAI_CODEX_CLI("openai-codex-cli"),

    /** Fournisseur absent ou mode dégradé. */
    NONE("none");

    private final String value;

    LlmProvider(final String value) {
        this.value = value;
    }

    /**
     * Valeur métier du fournisseur, utilisée dans la configuration et les APIs.
     * Annotée {@code @JsonValue} pour une sérialisation JSON transparente.
     */
    @JsonValue
    public String value() {
        return value;
    }

    /**
     * Convertit une chaîne de configuration en {@link LlmProvider}.
     *
     * <p>La comparaison est insensible à la casse. Si la valeur est inconnue,
     * retourne {@link #NONE} plutôt que de lever une exception (degradation gracieuse).
     *
     * @param value valeur à convertir (ex. "claude-code", "CLAUDE_CODE_CLI")
     * @return le LlmProvider correspondant, ou {@link #NONE} si inconnu
     */
    public static LlmProvider fromString(final String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        String normalized = value.trim().toLowerCase();
        for (LlmProvider provider : values()) {
            if (provider.value.equalsIgnoreCase(normalized)
                    || provider.name().equalsIgnoreCase(normalized)) {
                return provider;
            }
        }
        return NONE;
    }
}
