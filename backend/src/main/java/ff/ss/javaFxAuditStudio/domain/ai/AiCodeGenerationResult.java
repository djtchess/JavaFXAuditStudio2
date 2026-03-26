package ff.ss.javaFxAuditStudio.domain.ai;

import java.util.Map;
import java.util.Objects;

/**
 * Résultat de la génération IA des classes cibles Spring Boot (JAS-031 / IAP-2).
 *
 * <p>Les classes générées sont indexées par type d'artefact :
 * {@code USE_CASE}, {@code VIEW_MODEL}, {@code POLICY}, {@code GATEWAY}.
 * Chaque valeur est le code Java complet de la classe générée.
 *
 * <p>Si {@code degraded} est vrai, {@code generatedClasses} est vide et
 * {@code degradationReason} indique la cause.
 *
 * @param requestId         UUID de corrélation
 * @param degraded          Vrai si le mode dégradé est actif
 * @param degradationReason Raison du mode dégradé, vide si nominal
 * @param generatedClasses  Clé = type d'artefact, valeur = code Java généré complet
 * @param tokensUsed        Tokens consommés (0 en mode dégradé)
 * @param provider          Fournisseur IA utilisé (enum typesafe)
 */
public record AiCodeGenerationResult(
        String requestId,
        boolean degraded,
        String degradationReason,
        Map<String, String> generatedClasses,
        int tokensUsed,
        LlmProvider provider) {

    public AiCodeGenerationResult {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        degradationReason = degradationReason != null ? degradationReason : "";
        generatedClasses = generatedClasses != null ? Map.copyOf(generatedClasses) : Map.of();
    }

    /**
     * Crée un résultat dégradé avec classes générées vides.
     */
    public static AiCodeGenerationResult degraded(final String requestId, final String reason) {
        Objects.requireNonNull(requestId, "requestId must not be null");
        String safeReason = (reason != null) ? reason : "Raison inconnue";
        return new AiCodeGenerationResult(requestId, true, safeReason, Map.of(), 0, LlmProvider.NONE);
    }
}
