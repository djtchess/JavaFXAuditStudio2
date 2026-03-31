package ff.ss.javaFxAuditStudio.adapters.in.rest;

import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ExportArtifactsRequest;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ExportArtifactsResponse;
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
import ff.ss.javaFxAuditStudio.application.ports.in.ExportArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.GetAnalysisSessionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceMigrationPlanUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceRestitutionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.SubmitAnalysisUseCase;
import ff.ss.javaFxAuditStudio.domain.generation.ExportResult;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import ff.ss.javaFxAuditStudio.domain.workbench.OrchestratedAnalysisResult;

@Tag(name = "Analyse", description = "Sessions d'analyse JavaFX et orchestration du pipeline")
@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final CartographyUseCase cartographyUseCase;
    private final ClassifyResponsibilitiesUseCase classifyResponsibilitiesUseCase;
    private final GenerateArtifactsUseCase generateArtifactsUseCase;
    private final ProduceMigrationPlanUseCase produceMigrationPlanUseCase;
    private final ProduceRestitutionUseCase produceRestitutionUseCase;
    private final SubmitAnalysisUseCase submitAnalysisUseCase;
    private final GetAnalysisSessionUseCase getAnalysisSessionUseCase;
    private final AnalysisSessionPort analysisSessionPort;
    private final AnalysisOrchestrationUseCase analysisOrchestrationUseCase;
    private final AnalysisSessionResponseMapper analysisSessionResponseMapper;
    private final CartographyResponseMapper cartographyResponseMapper;
    private final ClassificationResponseMapper classificationResponseMapper;
    private final MigrationPlanResponseMapper migrationPlanResponseMapper;
    private final ArtifactsResponseMapper artifactsResponseMapper;
    private final RestitutionReportResponseMapper restitutionReportResponseMapper;
    private final OrchestratedAnalysisResultResponseMapper orchestratedAnalysisResultResponseMapper;
    private final ExportArtifactsUseCase exportArtifactsUseCase;

    public AnalysisController(
            final CartographyUseCase cartographyUseCase,
            final ClassifyResponsibilitiesUseCase classifyResponsibilitiesUseCase,
            final GenerateArtifactsUseCase generateArtifactsUseCase,
            final ProduceMigrationPlanUseCase produceMigrationPlanUseCase,
            final ProduceRestitutionUseCase produceRestitutionUseCase,
            final SubmitAnalysisUseCase submitAnalysisUseCase,
            final GetAnalysisSessionUseCase getAnalysisSessionUseCase,
            final AnalysisSessionPort analysisSessionPort,
            final AnalysisOrchestrationUseCase analysisOrchestrationUseCase,
            final AnalysisSessionResponseMapper analysisSessionResponseMapper,
            final CartographyResponseMapper cartographyResponseMapper,
            final ClassificationResponseMapper classificationResponseMapper,
            final MigrationPlanResponseMapper migrationPlanResponseMapper,
            final ArtifactsResponseMapper artifactsResponseMapper,
            final RestitutionReportResponseMapper restitutionReportResponseMapper,
            final OrchestratedAnalysisResultResponseMapper orchestratedAnalysisResultResponseMapper,
            final ExportArtifactsUseCase exportArtifactsUseCase) {
        this.cartographyUseCase = cartographyUseCase;
        this.classifyResponsibilitiesUseCase = classifyResponsibilitiesUseCase;
        this.generateArtifactsUseCase = generateArtifactsUseCase;
        this.produceMigrationPlanUseCase = produceMigrationPlanUseCase;
        this.produceRestitutionUseCase = produceRestitutionUseCase;
        this.submitAnalysisUseCase = submitAnalysisUseCase;
        this.getAnalysisSessionUseCase = getAnalysisSessionUseCase;
        this.analysisSessionPort = analysisSessionPort;
        this.analysisOrchestrationUseCase = analysisOrchestrationUseCase;
        this.analysisSessionResponseMapper = analysisSessionResponseMapper;
        this.cartographyResponseMapper = cartographyResponseMapper;
        this.classificationResponseMapper = classificationResponseMapper;
        this.migrationPlanResponseMapper = migrationPlanResponseMapper;
        this.artifactsResponseMapper = artifactsResponseMapper;
        this.restitutionReportResponseMapper = restitutionReportResponseMapper;
        this.orchestratedAnalysisResultResponseMapper = orchestratedAnalysisResultResponseMapper;
        this.exportArtifactsUseCase = exportArtifactsUseCase;
    }

    @Operation(summary = "Creer une session d'analyse", description = "Soumet des fichiers source JavaFX pour analyse. Cree une session en statut CREATED.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Session creee"),
        @ApiResponse(responseCode = "400", description = "Requete invalide")
    })
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisSessionResponse submitSession(@RequestBody final SubmitAnalysisRequest request) {
        AnalysisSession session = submitAnalysisUseCase.handle(request.sourceFilePaths(), request.sessionName());
        return analysisSessionResponseMapper.toResponse(session);
    }

    @Operation(summary = "Consulter une session d'analyse", description = "Retourne le statut courant et les references associees a une session.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Session trouvee"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<AnalysisSessionResponse> getSession(
            @Parameter(name = "sessionId", description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId) {
        return getAnalysisSessionUseCase.handle(sessionId)
                .map(session -> ResponseEntity.ok(analysisSessionResponseMapper.toResponse(session)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Cartographie FXML", description = "Retourne l'inventaire des composants FXML et les bindings handlers de la session.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cartographie disponible"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @GetMapping("/sessions/{sessionId}/cartography")
    public CartographyResponse getCartography(
            @Parameter(name = "sessionId", description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId) {
        AnalysisSession session = analysisSessionPort.findById(sessionId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session introuvable : " + sessionId));
        return cartographyResponseMapper.toResponse(
                cartographyUseCase.handle(sessionId, session.controllerName(), session.sourceSnippetRef()));
    }

    @Operation(summary = "Regles metier classifiees", description = "Retourne la liste des regles metier extraites et leur categorie (USE_CASE, GATEWAY, POLICY, VIEW_MODEL, LIFECYCLE).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Classification disponible"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @GetMapping("/sessions/{sessionId}/classification")
    public ClassificationResponse getClassification(
            @Parameter(name = "sessionId", description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId) {
        AnalysisSession session = analysisSessionPort.findById(sessionId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session introuvable : " + sessionId));
        return classificationResponseMapper.toResponse(
                classifyResponsibilitiesUseCase.handle(sessionId, session.controllerName()));
    }

    @Operation(summary = "Plan de migration", description = "Retourne le plan de migration par lots et la compilabilite des artefacts.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plan disponible"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @GetMapping("/sessions/{sessionId}/plan")
    public MigrationPlanResponse getMigrationPlan(
            @Parameter(name = "sessionId", description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId) {
        AnalysisSession session = analysisSessionPort.findById(sessionId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session introuvable : " + sessionId));
        return migrationPlanResponseMapper.toResponse(
                produceMigrationPlanUseCase.handle(sessionId, session.controllerName()));
    }

    @Operation(summary = "Artefacts generes", description = "Retourne les artefacts de code Java generes (UseCase, Gateway, ViewModel, Policy, tests).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Artefacts disponibles"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @GetMapping("/sessions/{sessionId}/artifacts")
    public ArtifactsResponse getArtifacts(
            @Parameter(name = "sessionId", description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId) {
        AnalysisSession session = analysisSessionPort.findById(sessionId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session introuvable : " + sessionId));
        return artifactsResponseMapper.toResponse(
                generateArtifactsUseCase.handle(sessionId, session.controllerName()));
    }

    @Operation(summary = "Rapport de restitution", description = "Retourne le rapport synthetique de l'analyse avec les recommandations.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rapport disponible"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @GetMapping("/sessions/{sessionId}/report")
    public RestitutionReportResponse getReport(
            @Parameter(name = "sessionId", description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId) {
        AnalysisSession session = analysisSessionPort.findById(sessionId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session introuvable : " + sessionId));
        return restitutionReportResponseMapper.toResponse(
                produceRestitutionUseCase.handle(sessionId, session.controllerName()));
    }

    @Operation(summary = "Exporter les artefacts", description = "Exporte les fichiers Java generes dans un repertoire cible.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Export effectue"),
        @ApiResponse(responseCode = "400", description = "Requete invalide"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @PostMapping("/sessions/{sessionId}/artifacts/export")
    public ExportArtifactsResponse exportArtifacts(
            @Parameter(name = "sessionId", description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId,
            @RequestBody final ExportArtifactsRequest request) {
        ExportResult result = exportArtifactsUseCase.export(sessionId, request.targetDirectory());
        return new ExportArtifactsResponse(result.targetDirectory(), result.exportedFiles(), result.errors());
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
    @Operation(summary = "Executer le pipeline complet", description = "Lance l'orchestration complete : cartographie, classification, generation et restitution. Retourne 202 si la session est deja en cours.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analyse terminee"),
        @ApiResponse(responseCode = "404", description = "Session introuvable"),
        @ApiResponse(responseCode = "409", description = "Session deja en cours ou terminee")
    })
    @PostMapping("/sessions/{sessionId}/run")
    public ResponseEntity<OrchestratedAnalysisResultResponse> runPipeline(
            @Parameter(name = "sessionId", description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId) {

        Optional<AnalysisSession> sessionOpt = analysisSessionPort.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (sessionOpt.get().status() != AnalysisStatus.CREATED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        OrchestratedAnalysisResult result = analysisOrchestrationUseCase.orchestrate(sessionId);
        return ResponseEntity.ok(orchestratedAnalysisResultResponseMapper.toResponse(result));
    }
}
