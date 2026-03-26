package ff.ss.javaFxAuditStudio.domain.ai;

import java.util.Map;
import java.util.Objects;

/**
 * Résultat d'un enrichissement IA (IAP-2).
 *
 * <p>Si {@code degraded} est vrai, {@code degradationReason} indique la cause.
 * Les suggestions sont vides en mode dégradé.
 *
 * @param requestId         UUID de corrélation
 * @param degraded          Vrai si mode dégradé actif
 * @param degradationReason Raison du mode dégradé, vide si nominal
 * @param suggestions       Clé = handlerName, valeur = suggestion
 * @param tokensUsed        Nombre de tokens utilisés (0 en mode dégradé)
 * @param provider          Fournisseur LLM utilisé (enum typesafe)
 */
public record AiEnrichmentResult(
        String requestId,
        boolean degraded,
        String degradationReason,
        Map<String, String> suggestions,
        int tokensUsed,
        LlmProvider provider) {

    public AiEnrichmentResult {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        degradationReason = (degradationReason != null) ? degradationReason : "";
        suggestions = (suggestions != null) ? Map.copyOf(suggestions) : Map.of();
    }

    /**
     * Crée un résultat dégradé avec {@link LlmProvider#NONE} comme fournisseur.
     */
    public static AiEnrichmentResult degraded(final String requestId, final String reason) {
        return degraded(requestId, reason, LlmProvider.NONE);
    }

    /**
     * Crée un résultat dégradé avec le fournisseur spécifié.
     */
    public static AiEnrichmentResult degraded(
            final String requestId, final String reason, final LlmProvider provider) {
        Objects.requireNonNull(requestId, "requestId must not be null");
        String safeReason = (reason != null) ? reason : "Raison inconnue";
        LlmProvider safeProvider = (provider != null) ? provider : LlmProvider.NONE;
        return new AiEnrichmentResult(requestId, true, safeReason, Map.of(), 0, safeProvider);
    }

    /**
     * Surcharge de compatibilité acceptant une String provider (convertie via {@link LlmProvider#fromString}).
     */
    public static AiEnrichmentResult degraded(
            final String requestId, final String reason, final String providerValue) {
        return degraded(requestId, reason, LlmProvider.fromString(providerValue));
    }
}
