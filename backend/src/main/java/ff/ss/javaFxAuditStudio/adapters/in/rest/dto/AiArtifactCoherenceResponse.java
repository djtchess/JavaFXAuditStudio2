package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO REST de cohérence inter-artefacts.
 */
@Schema(description = "Résultat de vérification de cohérence des artefacts IA")
public record AiArtifactCoherenceResponse(
        String requestId,
        boolean degraded,
        String degradationReason,
        String summary,
        Map<String, String> artifactFindings,
        List<String> globalFindings,
        int tokensUsed,
        String provider) {
}
