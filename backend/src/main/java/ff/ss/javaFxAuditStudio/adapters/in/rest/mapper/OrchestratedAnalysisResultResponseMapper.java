package ff.ss.javaFxAuditStudio.adapters.in.rest.mapper;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ArtifactsResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.CartographyResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ClassificationResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.MigrationPlanResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.OrchestratedAnalysisResultResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.RestitutionReportResponse;
import ff.ss.javaFxAuditStudio.domain.workbench.OrchestratedAnalysisResult;

import org.springframework.stereotype.Component;

/**
 * Mapper REST pour {@link OrchestratedAnalysisResult}.
 * Delegue la conversion de chaque sous-objet aux mappers existants.
 * Les champs null (etapes non atteintes en cas d'echec) sont conserves null dans la reponse.
 */
@Component
public class OrchestratedAnalysisResultResponseMapper {

    private final CartographyResponseMapper cartographyResponseMapper;
    private final ClassificationResponseMapper classificationResponseMapper;
    private final MigrationPlanResponseMapper migrationPlanResponseMapper;
    private final ArtifactsResponseMapper artifactsResponseMapper;
    private final RestitutionReportResponseMapper restitutionReportResponseMapper;

    public OrchestratedAnalysisResultResponseMapper(
            final CartographyResponseMapper cartographyResponseMapper,
            final ClassificationResponseMapper classificationResponseMapper,
            final MigrationPlanResponseMapper migrationPlanResponseMapper,
            final ArtifactsResponseMapper artifactsResponseMapper,
            final RestitutionReportResponseMapper restitutionReportResponseMapper) {
        this.cartographyResponseMapper = cartographyResponseMapper;
        this.classificationResponseMapper = classificationResponseMapper;
        this.migrationPlanResponseMapper = migrationPlanResponseMapper;
        this.artifactsResponseMapper = artifactsResponseMapper;
        this.restitutionReportResponseMapper = restitutionReportResponseMapper;
    }

    public OrchestratedAnalysisResultResponse toResponse(final OrchestratedAnalysisResult result) {
        CartographyResponse cartographyResponse = result.cartography() != null
                ? cartographyResponseMapper.toResponse(result.cartography())
                : null;

        ClassificationResponse classificationResponse = result.classification() != null
                ? classificationResponseMapper.toResponse(result.classification())
                : null;

        MigrationPlanResponse migrationPlanResponse = result.migrationPlan() != null
                ? migrationPlanResponseMapper.toResponse(result.migrationPlan())
                : null;

        ArtifactsResponse artifactsResponse = result.generationResult() != null
                ? artifactsResponseMapper.toResponse(result.generationResult())
                : null;

        RestitutionReportResponse restitutionReportResponse = result.restitutionReport() != null
                ? restitutionReportResponseMapper.toResponse(result.restitutionReport())
                : null;

        return new OrchestratedAnalysisResultResponse(
                result.sessionId(),
                result.finalStatus().name(),
                cartographyResponse,
                classificationResponse,
                migrationPlanResponse,
                artifactsResponse,
                restitutionReportResponse,
                result.errors());
    }
}
