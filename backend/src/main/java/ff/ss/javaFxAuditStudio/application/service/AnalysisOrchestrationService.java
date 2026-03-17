package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.AnalysisOrchestrationUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.CartographyUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ClassifyResponsibilitiesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.IngestSourcesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceMigrationPlanUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceRestitutionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;
import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import ff.ss.javaFxAuditStudio.domain.workbench.OrchestratedAnalysisResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    public AnalysisOrchestrationService(
            final AnalysisSessionPort analysisSessionPort,
            final IngestSourcesUseCase ingestSourcesUseCase,
            final CartographyUseCase cartographyUseCase,
            final ClassifyResponsibilitiesUseCase classifyResponsibilitiesUseCase,
            final ProduceMigrationPlanUseCase produceMigrationPlanUseCase,
            final GenerateArtifactsUseCase generateArtifactsUseCase,
            final ProduceRestitutionUseCase produceRestitutionUseCase) {
        this.analysisSessionPort = Objects.requireNonNull(analysisSessionPort, "analysisSessionPort must not be null");
        this.ingestSourcesUseCase = Objects.requireNonNull(ingestSourcesUseCase, "ingestSourcesUseCase must not be null");
        this.cartographyUseCase = Objects.requireNonNull(cartographyUseCase, "cartographyUseCase must not be null");
        this.classifyResponsibilitiesUseCase = Objects.requireNonNull(classifyResponsibilitiesUseCase, "classifyResponsibilitiesUseCase must not be null");
        this.produceMigrationPlanUseCase = Objects.requireNonNull(produceMigrationPlanUseCase, "produceMigrationPlanUseCase must not be null");
        this.generateArtifactsUseCase = Objects.requireNonNull(generateArtifactsUseCase, "generateArtifactsUseCase must not be null");
        this.produceRestitutionUseCase = Objects.requireNonNull(produceRestitutionUseCase, "produceRestitutionUseCase must not be null");
    }

    @Override
    public OrchestratedAnalysisResult orchestrate(final String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        log.info("Orchestration demarree - sessionId masque");

        // Etape 1 : retrouver la session
        Optional<AnalysisSession> sessionOpt = analysisSessionPort.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            log.warn("Session introuvable lors de l'orchestration - sessionId masque");
            return new OrchestratedAnalysisResult(
                    sessionId,
                    AnalysisStatus.FAILED,
                    null, null, null, null, null,
                    List.of("Session introuvable : " + sessionId));
        }

        AnalysisSession session = sessionOpt.get();
        final String controllerRef = session.controllerName();
        final String sourceRef = session.sourceSnippetRef() != null ? session.sourceSnippetRef() : controllerRef;

        // Etape 2 : passer en IN_PROGRESS
        AnalysisSession sessionInProgress = new AnalysisSession(
                session.sessionId(),
                session.controllerName(),
                session.sourceSnippetRef(),
                AnalysisStatus.IN_PROGRESS,
                session.createdAt());
        analysisSessionPort.save(sessionInProgress);

        try {
            // Etape 3 : ingestion
            log.debug("Etape ingestion demarree");
            ingestSourcesUseCase.handle(List.of(sourceRef));

            // Etape 4 : cartographie — utilise les chemins reels stockes dans la session
            log.debug("Etape cartographie demarree");
            ControllerCartography cartography = cartographyUseCase.handle(
                    sessionId, session.controllerName(), session.sourceSnippetRef());

            // Etape 5 : classification
            log.debug("Etape classification demarree");
            ClassificationResult classification = classifyResponsibilitiesUseCase.handle(sessionId, cartography.controllerRef());

            // Etape 6 : plan de migration
            log.debug("Etape plan de migration demarree");
            MigrationPlan migrationPlan = produceMigrationPlanUseCase.handle(sessionId, classification.controllerRef());

            // Etape 7 : generation
            log.debug("Etape generation demarree");
            GenerationResult generationResult = generateArtifactsUseCase.handle(sessionId, migrationPlan.controllerRef());

            // Etape 8 : restitution
            log.debug("Etape restitution demarree");
            RestitutionReport restitutionReport = produceRestitutionUseCase.handle(sessionId, generationResult.controllerRef());

            // Etape 9 : COMPLETED
            AnalysisSession sessionCompleted = new AnalysisSession(
                    session.sessionId(),
                    session.controllerName(),
                    session.sourceSnippetRef(),
                    AnalysisStatus.COMPLETED,
                    session.createdAt());
            analysisSessionPort.save(sessionCompleted);

            log.info("Orchestration terminee avec succes");
            return new OrchestratedAnalysisResult(
                    sessionId,
                    AnalysisStatus.COMPLETED,
                    cartography,
                    classification,
                    migrationPlan,
                    generationResult,
                    restitutionReport,
                    List.of());

        } catch (Exception ex) {
            log.error("Orchestration echouee - etape inconnue", ex);
            AnalysisSession sessionFailed = new AnalysisSession(
                    session.sessionId(),
                    session.controllerName(),
                    session.sourceSnippetRef(),
                    AnalysisStatus.FAILED,
                    session.createdAt());
            analysisSessionPort.save(sessionFailed);

            return new OrchestratedAnalysisResult(
                    sessionId,
                    AnalysisStatus.FAILED,
                    null, null, null, null, null,
                    List.of(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName()));
        }
    }
}
