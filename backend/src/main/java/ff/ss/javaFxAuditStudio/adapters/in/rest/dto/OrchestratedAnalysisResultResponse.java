package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * Representation REST du resultat d'orchestration bout-en-bout.
 * Agrege les informations pertinentes de chaque etape du pipeline.
 */
@Schema(description = "Resultat complet du pipeline d'analyse orchestre")
public record OrchestratedAnalysisResultResponse(
        @Schema(description = "Identifiant unique de la session d'analyse (UUID)")
        String sessionId,
        @Schema(description = "Statut final du pipeline (ex: COMPLETED, FAILED)")
        String finalStatus,
        @Schema(description = "Resultat de la cartographie FXML, null si etape non atteinte", nullable = true)
        CartographyResponse cartography,
        @Schema(description = "Resultat de la classification des regles, null si etape non atteinte", nullable = true)
        ClassificationResponse classification,
        @Schema(description = "Plan de migration genere, null si etape non atteinte", nullable = true)
        MigrationPlanResponse migrationPlan,
        @Schema(description = "Artefacts de code generes, null si etape non atteinte", nullable = true)
        ArtifactsResponse generationResult,
        @Schema(description = "Rapport de restitution synthetique, null si etape non atteinte", nullable = true)
        RestitutionReportResponse restitutionReport,
        @Schema(description = "Liste des erreurs survenues durant le pipeline")
        List<String> errors) {

    public OrchestratedAnalysisResultResponse {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(finalStatus, "finalStatus must not be null");
        Objects.requireNonNull(errors, "errors must not be null");
        errors = List.copyOf(errors);
    }
}
