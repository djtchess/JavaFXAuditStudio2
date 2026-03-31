package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.AnalysisOrchestrationUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.CartographyUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ClassifyResponsibilitiesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.IngestSourcesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceMigrationPlanUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceRestitutionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionStatusHistoryPort;
import ff.ss.javaFxAuditStudio.application.ports.out.WorkflowObservabilityPort;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;
import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatusTransition;
import ff.ss.javaFxAuditStudio.domain.workbench.OrchestratedAnalysisResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Service applicatif orchestrant le pipeline bout-en-bout d'une session d'analyse.
 * Implementer {@link AnalysisOrchestrationUseCase}.
 *
 * <p>Aucune dependance Spring, JPA ou adapter dans cette classe.
 * L'assemblage des dependances est delegue a {@code AnalysisOrchestrationConfiguration}.
 */
public final class AnalysisOrchestrationService implements AnalysisOrchestrationUseCase {

    private static final Logger log = LoggerFactory.getLogger(AnalysisOrchestrationService.class);

    private final AnalysisSessionPort analysisSessionPort;
    private final IngestSourcesUseCase ingestSourcesUseCase;
    private final CartographyUseCase cartographyUseCase;
    private final ClassifyResponsibilitiesUseCase classifyResponsibilitiesUseCase;
    private final ProduceMigrationPlanUseCase produceMigrationPlanUseCase;
    private final GenerateArtifactsUseCase generateArtifactsUseCase;
    private final ProduceRestitutionUseCase produceRestitutionUseCase;
    private final AnalysisSessionStatusHistoryPort statusHistoryPort;
    private final WorkflowObservabilityPort observabilityPort;

    public AnalysisOrchestrationService(
            final AnalysisSessionPort analysisSessionPort,
            final IngestSourcesUseCase ingestSourcesUseCase,
            final CartographyUseCase cartographyUseCase,
            final ClassifyResponsibilitiesUseCase classifyResponsibilitiesUseCase,
            final ProduceMigrationPlanUseCase produceMigrationPlanUseCase,
            final GenerateArtifactsUseCase generateArtifactsUseCase,
            final ProduceRestitutionUseCase produceRestitutionUseCase) {
        this(
                analysisSessionPort,
                ingestSourcesUseCase,
                cartographyUseCase,
                classifyResponsibilitiesUseCase,
                produceMigrationPlanUseCase,
                generateArtifactsUseCase,
                produceRestitutionUseCase,
                AnalysisSessionStatusHistoryPort.noop(),
                WorkflowObservabilityPort.noop());
    }

    public AnalysisOrchestrationService(
            final AnalysisSessionPort analysisSessionPort,
            final IngestSourcesUseCase ingestSourcesUseCase,
            final CartographyUseCase cartographyUseCase,
            final ClassifyResponsibilitiesUseCase classifyResponsibilitiesUseCase,
            final ProduceMigrationPlanUseCase produceMigrationPlanUseCase,
            final GenerateArtifactsUseCase generateArtifactsUseCase,
            final ProduceRestitutionUseCase produceRestitutionUseCase,
            final AnalysisSessionStatusHistoryPort statusHistoryPort,
            final WorkflowObservabilityPort observabilityPort) {
        this.analysisSessionPort = Objects.requireNonNull(analysisSessionPort, "analysisSessionPort must not be null");
        this.ingestSourcesUseCase = Objects.requireNonNull(ingestSourcesUseCase, "ingestSourcesUseCase must not be null");
        this.cartographyUseCase = Objects.requireNonNull(cartographyUseCase, "cartographyUseCase must not be null");
        this.classifyResponsibilitiesUseCase = Objects.requireNonNull(classifyResponsibilitiesUseCase, "classifyResponsibilitiesUseCase must not be null");
        this.produceMigrationPlanUseCase = Objects.requireNonNull(produceMigrationPlanUseCase, "produceMigrationPlanUseCase must not be null");
        this.generateArtifactsUseCase = Objects.requireNonNull(generateArtifactsUseCase, "generateArtifactsUseCase must not be null");
        this.produceRestitutionUseCase = Objects.requireNonNull(produceRestitutionUseCase, "produceRestitutionUseCase must not be null");
        this.statusHistoryPort = (statusHistoryPort != null) ? statusHistoryPort : AnalysisSessionStatusHistoryPort.noop();
        this.observabilityPort = (observabilityPort != null) ? observabilityPort : WorkflowObservabilityPort.noop();
    }

    @Override
    public OrchestratedAnalysisResult orchestrate(final String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Instant startedAt = Instant.now();
        log.info("Orchestration demarree - sessionId masque");

        Optional<AnalysisSession> sessionOpt = analysisSessionPort.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            log.warn("Session introuvable lors de l'orchestration - sessionId masque");
            observabilityPort.recordPipelineOutcome("not_found", Duration.between(startedAt, Instant.now()));
            return buildFailureResult(sessionId, "Session introuvable : " + sessionId);
        }

        AnalysisSession session = sessionOpt.get();
        String sourceRef = session.sourceSnippetRef() != null ? session.sourceSnippetRef() : session.controllerName();

        try {
            saveStatus(session, AnalysisStatus.IN_PROGRESS);
            ingestSources(session, sourceRef);
            ControllerCartography cartography = cartography(sessionId, session);
            ClassificationResult classification = classify(sessionId, session, cartography);
            MigrationPlan migrationPlan = planMigration(sessionId, session, classification);
            GenerationResult generationResult = generateArtifacts(sessionId, session, migrationPlan);
            RestitutionReport restitutionReport = produceRestitution(sessionId, session, generationResult);
            saveStatus(session, AnalysisStatus.COMPLETED);
            log.info("Orchestration terminee avec succes");
            observabilityPort.recordPipelineOutcome("success", Duration.between(startedAt, Instant.now()));
            return buildSuccessResult(
                    sessionId,
                    cartography,
                    classification,
                    migrationPlan,
                    generationResult,
                    restitutionReport);
        } catch (Exception ex) {
            log.error("Orchestration echouee - etape inconnue", ex);
            saveStatus(session, AnalysisStatus.FAILED);
            observabilityPort.recordPipelineOutcome("failure", Duration.between(startedAt, Instant.now()));
            return buildFailureResult(sessionId, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName());
        }
    }

    private void ingestSources(final AnalysisSession session, final String sourceRef) {
        executeStage(
                "ingest",
                session,
                AnalysisStatus.INGESTING,
                () -> {
                    log.debug("Etape ingestion demarree");
                    ingestSourcesUseCase.handle(List.of(sourceRef));
                    return null;
                });
    }

    private ControllerCartography cartography(final String sessionId, final AnalysisSession session) {
        return executeStage(
                "cartography",
                session,
                AnalysisStatus.CARTOGRAPHING,
                () -> {
                    log.debug("Etape cartographie demarree");
                    return cartographyUseCase.handle(sessionId, session.controllerName(), session.sourceSnippetRef());
                });
    }

    private ClassificationResult classify(
            final String sessionId,
            final AnalysisSession session,
            final ControllerCartography cartography) {
        return executeStage(
                "classification",
                session,
                AnalysisStatus.CLASSIFYING,
                () -> {
                    log.debug("Etape classification demarree");
                    return classifyResponsibilitiesUseCase.handle(sessionId, cartography.controllerRef());
                });
    }

    private MigrationPlan planMigration(
            final String sessionId,
            final AnalysisSession session,
            final ClassificationResult classification) {
        return executeStage(
                "planning",
                session,
                AnalysisStatus.PLANNING,
                () -> {
                    log.debug("Etape plan de migration demarree");
                    return produceMigrationPlanUseCase.handle(sessionId, classification.controllerRef());
                });
    }

    private GenerationResult generateArtifacts(
            final String sessionId,
            final AnalysisSession session,
            final MigrationPlan migrationPlan) {
        return executeStage(
                "generation",
                session,
                AnalysisStatus.GENERATING,
                () -> {
                    log.debug("Etape generation demarree");
                    return generateArtifactsUseCase.handle(sessionId, migrationPlan.controllerRef());
                });
    }

    private RestitutionReport produceRestitution(
            final String sessionId,
            final AnalysisSession session,
            final GenerationResult generationResult) {
        return executeStage(
                "reporting",
                session,
                AnalysisStatus.REPORTING,
                () -> {
                    log.debug("Etape restitution demarree");
                    return produceRestitutionUseCase.handle(sessionId, generationResult.controllerRef());
                });
    }

    private <T> T executeStage(
            final String stageName,
            final AnalysisSession session,
            final AnalysisStatus status,
            final Supplier<T> action) {
        saveStatus(session, status);
        Instant startedAt = Instant.now();
        try {
            T result = action.get();
            observabilityPort.recordPipelineStage(stageName, "success", Duration.between(startedAt, Instant.now()));
            return result;
        } catch (RuntimeException ex) {
            observabilityPort.recordPipelineStage(stageName, "failure", Duration.between(startedAt, Instant.now()));
            throw ex;
        }
    }

    private AnalysisSession saveStatus(final AnalysisSession session, final AnalysisStatus status) {
        AnalysisSession updatedSession = session.withStatus(status);
        Instant occurredAt = Instant.now();

        analysisSessionPort.save(updatedSession);
        statusHistoryPort.save(new AnalysisStatusTransition(updatedSession.sessionId(), status, occurredAt));
        return updatedSession;
    }

    private OrchestratedAnalysisResult buildSuccessResult(
            final String sessionId,
            final ControllerCartography cartography,
            final ClassificationResult classification,
            final MigrationPlan migrationPlan,
            final GenerationResult generationResult,
            final RestitutionReport restitutionReport) {
        return new OrchestratedAnalysisResult(
                sessionId,
                AnalysisStatus.COMPLETED,
                cartography,
                classification,
                migrationPlan,
                generationResult,
                restitutionReport,
                List.of());
    }

    private OrchestratedAnalysisResult buildFailureResult(final String sessionId, final String errorMessage) {
        return new OrchestratedAnalysisResult(
                sessionId,
                AnalysisStatus.FAILED,
                null, null, null, null, null,
                List.of(errorMessage));
    }
}
