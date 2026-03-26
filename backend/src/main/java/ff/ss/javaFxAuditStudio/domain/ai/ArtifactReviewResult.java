package ff.ss.javaFxAuditStudio.domain.ai;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Résultat d'une revue IA des artefacts générés (JAS-030 / IAP-2).
 *
 * @param requestId                  UUID de corrélation
 * @param degraded                   Vrai si mode dégradé actif
 * @param degradationReason          Raison du mode dégradé
 * @param migrationScore             Score de qualité de migration (0-100), -1 si indisponible
 * @param artifactReviews            Clé = type d'artefact, valeur = commentaire
 * @param uncertainReclassifications Clé = ruleId, valeur = suggestion de reclassification
 * @param globalSuggestions          Suggestions globales de migration
 * @param provider                   Fournisseur IA utilisé (enum typesafe)
 */
public record ArtifactReviewResult(
        String requestId,
        boolean degraded,
        String degradationReason,
        int migrationScore,
        Map<String, String> artifactReviews,
        Map<String, String> uncertainReclassifications,
        List<String> globalSuggestions,
        LlmProvider provider) {

    public ArtifactReviewResult {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        degradationReason = degradationReason != null ? degradationReason : "";
        artifactReviews = artifactReviews != null ? Map.copyOf(artifactReviews) : Map.of();
        uncertainReclassifications = uncertainReclassifications != null ? Map.copyOf(uncertainReclassifications) : Map.of();
        globalSuggestions = globalSuggestions != null ? List.copyOf(globalSuggestions) : List.of();
    }

    public static ArtifactReviewResult degraded(final String requestId, final String reason) {
        return new ArtifactReviewResult(
                requestId, true, reason, -1, Map.of(), Map.of(), List.of(), LlmProvider.NONE);
    }
}
