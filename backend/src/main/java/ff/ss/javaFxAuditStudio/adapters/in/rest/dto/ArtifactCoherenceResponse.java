package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * DTO de reponse pour la verification de coherence inter-artefacts.
 */
@Schema(description = "Resultat de la verification de coherence inter-artefacts")
public record ArtifactCoherenceResponse(
        @Schema(description = "UUID de correlation")
        String requestId,
        @Schema(description = "Vrai si le mode degrade est actif")
        boolean degraded,
        @Schema(description = "Raison du mode degrade")
        String degradationReason,
        @Schema(description = "Vrai si les artefacts sont coherents")
        boolean coherent,
        @Schema(description = "Constats de coherence par type d'artefact")
        Map<String, String> artifactIssues,
        @Schema(description = "Suggestions globales")
        List<String> globalSuggestions,
        @Schema(description = "Fournisseur IA utilise")
        String provider) {
}
