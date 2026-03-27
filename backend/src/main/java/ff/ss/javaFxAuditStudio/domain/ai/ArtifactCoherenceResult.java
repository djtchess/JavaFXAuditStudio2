package ff.ss.javaFxAuditStudio.domain.ai;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resultat d'une verification de coherence inter-artefacts.
 *
 * @param requestId          UUID de correlation
 * @param degraded           Vrai si mode degrade actif
 * @param degradationReason   Raison du mode degrade
 * @param coherent           Vrai si les artefacts sont coherents entre eux
 * @param artifactIssues     Clé = type d'artefact, valeur = constat de coherence
 * @param globalSuggestions  Suggestions globales
 * @param provider           Fournisseur IA utilise
 */
public record ArtifactCoherenceResult(
        String requestId,
        boolean degraded,
        String degradationReason,
        boolean coherent,
        Map<String, String> artifactIssues,
        List<String> globalSuggestions,
        LlmProvider provider) {

    public ArtifactCoherenceResult {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        degradationReason = degradationReason != null ? degradationReason : "";
        artifactIssues = artifactIssues != null ? Map.copyOf(artifactIssues) : Map.of();
        globalSuggestions = globalSuggestions != null ? List.copyOf(globalSuggestions) : List.of();
    }

    public static ArtifactCoherenceResult degraded(final String requestId, final String reason) {
        return new ArtifactCoherenceResult(requestId, true, reason, false, Map.of(), List.of(), LlmProvider.NONE);
    }
}
