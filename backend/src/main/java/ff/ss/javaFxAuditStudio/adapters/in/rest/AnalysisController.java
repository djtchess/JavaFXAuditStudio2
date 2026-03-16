package ff.ss.javaFxAuditStudio.adapters.in.rest;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AnalysisSessionResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ArtifactsResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.CartographyResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ClassificationResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.MigrationPlanResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.OrchestratedAnalysisResultResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.RestitutionReportResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.SubmitAnalysisRequest;
import ff.ss.javaFxAuditStudio.adapters.in.rest.mapper.AnalysisSessionResponseMapper;
import ff.ss.javaFxAuditStudio.adapters.in.rest.mapper.ArtifactsResponseMapper;
import ff.ss.javaFxAuditStudio.adapters.in.rest.mapper.CartographyResponseMapper;
import ff.ss.javaFxAuditStudio.adapters.in.rest.mapper.ClassificationResponseMapper;
import ff.ss.javaFxAuditStudio.adapters.in.rest.mapper.MigrationPlanResponseMapper;
import ff.ss.javaFxAuditStudio.adapters.in.rest.mapper.OrchestratedAnalysisResultResponseMapper;
import ff.ss.javaFxAuditStudio.adapters.in.rest.mapper.RestitutionReportResponseMapper;
import ff.ss.javaFxAuditStudio.application.ports.in.AnalysisOrchestrationUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.CartographyUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ClassifyResponsibilitiesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceMigrationPlanUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceRestitutionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import ff.ss.javaFxAuditStudio.domain.workbench.OrchestratedAnalysisResult;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final CartographyUseCase cartographyUseCase;
    private final ClassifyResponsibilitiesUseCase classifyResponsibilitiesUseCase;
    private final GenerateArtifactsUseCase generateArtifactsUseCase;
    private final ProduceMigrationPlanUseCase produceMigrationPlanUseCase;
    private final ProduceRestitutionUseCase produceRestitutionUseCase;
    private final AnalysisSessionPort analysisSessionPort;
    private final AnalysisOrchestrationUseCase analysisOrchestrationUseCase;
    private final AnalysisSessionResponseMapper analysisSessionResponseMapper;
    private final CartographyResponseMapper cartographyResponseMapper;
    private final ClassificationResponseMapper classificationResponseMapper;
    private final MigrationPlanResponseMapper migrationPlanResponseMapper;
    private final ArtifactsResponseMapper artifactsResponseMapper;
    private final RestitutionReportResponseMapper restitutionReportResponseMapper;
    private final OrchestratedAnalysisResultResponseMapper orchestratedAnalysisResultResponseMapper;

    public AnalysisController(
            final CartographyUseCase cartographyUseCase,
            final ClassifyResponsibilitiesUseCase classifyResponsibilitiesUseCase,
            final GenerateArtifactsUseCase generateArtifactsUseCase,
            final ProduceMigrationPlanUseCase produceMigrationPlanUseCase,
            final ProduceRestitutionUseCase produceRestitutionUseCase,
            final AnalysisSessionPort analysisSessionPort,
            final AnalysisOrchestrationUseCase analysisOrchestrationUseCase,
            final AnalysisSessionResponseMapper analysisSessionResponseMapper,
            final CartographyResponseMapper cartographyResponseMapper,
            final ClassificationResponseMapper classificationResponseMapper,
            final MigrationPlanResponseMapper migrationPlanResponseMapper,
            final ArtifactsResponseMapper artifactsResponseMapper,
            final RestitutionReportResponseMapper restitutionReportResponseMapper,
            final OrchestratedAnalysisResultResponseMapper orchestratedAnalysisResultResponseMapper) {
        this.cartographyUseCase = cartographyUseCase;
        this.classifyResponsibilitiesUseCase = classifyResponsibilitiesUseCase;
        this.generateArtifactsUseCase = generateArtifactsUseCase;
        this.produceMigrationPlanUseCase = produceMigrationPlanUseCase;
        this.produceRestitutionUseCase = produceRestitutionUseCase;
        this.analysisSessionPort = analysisSessionPort;
        this.analysisOrchestrationUseCase = analysisOrchestrationUseCase;
        this.analysisSessionResponseMapper = analysisSessionResponseMapper;
        this.cartographyResponseMapper = cartographyResponseMapper;
        this.classificationResponseMapper = classificationResponseMapper;
        this.migrationPlanResponseMapper = migrationPlanResponseMapper;
        this.artifactsResponseMapper = artifactsResponseMapper;
        this.restitutionReportResponseMapper = restitutionReportResponseMapper;
        this.orchestratedAnalysisResultResponseMapper = orchestratedAnalysisResultResponseMapper;
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisSessionResponse submitSession(@RequestBody final SubmitAnalysisRequest request) {
        AnalysisSession session = new AnalysisSession(
                UUID.randomUUID().toString(),
                request.sessionName(),
                request.sourceFilePaths().get(0),
                AnalysisStatus.CREATED,
                Instant.now());
        analysisSessionPort.save(session);
        return analysisSessionResponseMapper.toResponse(session);
    }

    @GetMapping("/sessions/{sessionId}/cartography")
    public CartographyResponse getCartography(@PathVariable final String sessionId) {
        return cartographyResponseMapper.toResponse(
                cartographyUseCase.handle(sessionId, ""));
    }

    @GetMapping("/sessions/{sessionId}/classification")
    public ClassificationResponse getClassification(@PathVariable final String sessionId) {
        return classificationResponseMapper.toResponse(
                classifyResponsibilitiesUseCase.handle(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/plan")
    public MigrationPlanResponse getMigrationPlan(@PathVariable final String sessionId) {
        return migrationPlanResponseMapper.toResponse(
                produceMigrationPlanUseCase.handle(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/artifacts")
    public ArtifactsResponse getArtifacts(@PathVariable final String sessionId) {
        return artifactsResponseMapper.toResponse(
                generateArtifactsUseCase.handle(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/report")
    public RestitutionReportResponse getReport(@PathVariable final String sessionId) {
        return restitutionReportResponseMapper.toResponse(
                produceRestitutionUseCase.handle(sessionId));
    }

    /**
     * Lance le pipeline bout-en-bout pour la session identifiee.
     *
     * <ul>
     *   <li>200 OK avec le resultat complet si le pipeline aboutit.</li>
     *   <li>404 si la session est introuvable.</li>
     *   <li>409 si le statut de la session n'est pas compatible avec un demarrage
     *       (seul {@code CREATED} est accepte).</li>
     * </ul>
     */
    @PostMapping("/sessions/{sessionId}/run")
    public ResponseEntity<OrchestratedAnalysisResultResponse> runPipeline(
            @PathVariable final String sessionId) {

        return analysisSessionPort.findById(sessionId)
                .<ResponseEntity<OrchestratedAnalysisResultResponse>>map(session -> {
                    if (session.status() != AnalysisStatus.CREATED) {
                        return ResponseEntity
                                .<OrchestratedAnalysisResultResponse>status(HttpStatus.CONFLICT)
                                .build();
                    }
                    OrchestratedAnalysisResult result = analysisOrchestrationUseCase.orchestrate(sessionId);
                    return ResponseEntity.ok(orchestratedAnalysisResultResponseMapper.toResponse(result));
                })
                .orElse(ResponseEntity.<OrchestratedAnalysisResultResponse>notFound().build());
    }
}
