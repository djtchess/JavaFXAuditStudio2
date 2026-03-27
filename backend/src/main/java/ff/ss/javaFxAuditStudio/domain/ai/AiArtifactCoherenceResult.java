package ff.ss.javaFxAuditStudio.domain.ai;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Résultat d'une vérification de cohérence des artefacts IA.
 */
public record AiArtifactCoherenceResult(
        String requestId,
        boolean degraded,
        String degradationReason,
        String summary,
        Map<String, String> artifactFindings,
        List<String> globalFindings,
        int tokensUsed,
        LlmProvider provider) {

    public AiArtifactCoherenceResult {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        degradationReason = (degradationReason != null) ? degradationReason : "";
        artifactFindings = (artifactFindings != null) ? Map.copyOf(artifactFindings) : Map.of();
        globalFindings = (globalFindings != null) ? List.copyOf(globalFindings) : List.of();
    }

    public static AiArtifactCoherenceResult degraded(final String requestId, final String reason) {
        Objects.requireNonNull(requestId, "requestId must not be null");
        return new AiArtifactCoherenceResult(
                requestId,
                true,
                (reason != null) ? reason : "Raison inconnue",
                "",
                Map.of(),
                List.of(),
                0,
                LlmProvider.NONE);
    }
}
