package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.List;
import java.util.Objects;

/**
 * Representation REST du resultat d'orchestration bout-en-bout.
 * Agrege les informations pertinentes de chaque etape du pipeline.
 */
public record OrchestratedAnalysisResultResponse(
        String sessionId,
        String finalStatus,
        CartographyResponse cartography,
        ClassificationResponse classification,
        MigrationPlanResponse migrationPlan,
        ArtifactsResponse generationResult,
        RestitutionReportResponse restitutionReport,
        List<String> errors) {

    public OrchestratedAnalysisResultResponse {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(finalStatus, "finalStatus must not be null");
        Objects.requireNonNull(errors, "errors must not be null");
        errors = List.copyOf(errors);
    }
}
