package ff.ss.javaFxAuditStudio.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import ff.ss.javaFxAuditStudio.domain.ingestion.IngestionResult;
import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInput;
import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInputType;
import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;
import ff.ss.javaFxAuditStudio.domain.restitution.ConfidenceLevel;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionSummary;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;

@ExtendWith(MockitoExtension.class)
class AnalysisOrchestrationServiceObservabilityTest {

    @Mock
    private AnalysisSessionPort analysisSessionPort;

    @Mock
    private IngestSourcesUseCase ingestSourcesUseCase;

    @Mock
    private CartographyUseCase cartographyUseCase;

    @Mock
    private ClassifyResponsibilitiesUseCase classifyResponsibilitiesUseCase;

    @Mock
    private ProduceMigrationPlanUseCase produceMigrationPlanUseCase;

    @Mock
    private GenerateArtifactsUseCase generateArtifactsUseCase;

    @Mock
    private ProduceRestitutionUseCase produceRestitutionUseCase;

    @Mock
    private AnalysisSessionStatusHistoryPort statusHistoryPort;

    @Mock
    private WorkflowObservabilityPort workflowObservabilityPort;

    @Test
    void orchestrate_recordsAllStages_andOutcomeOnSuccess() {
        String sessionId;
        AnalysisOrchestrationService service;
        AnalysisSession session;
        ControllerCartography cartography;
        ClassificationResult classification;
        MigrationPlan migrationPlan;
        GenerationResult generationResult;
        RestitutionReport restitutionReport;

        sessionId = "session-ok";
        session = new AnalysisSession(
                sessionId,
                "com/example/MyController.java",
                "com/example/MyController.java",
                AnalysisStatus.CREATED,
                Instant.now());
        cartography = new ControllerCartography("controller-ref", "", List.of(), List.of(), List.of());
        classification = new ClassificationResult("controller-ref", List.of(), List.of());
        migrationPlan = new MigrationPlan("controller-ref", List.of(), true);
        generationResult = new GenerationResult("controller-ref", List.of(), List.of());
        restitutionReport = new RestitutionReport(
                new RestitutionSummary("controller-ref", 0, 0, 0, 0, ConfidenceLevel.HIGH, false),
                List.of(),
                List.of(),
                List.of());

        service = new AnalysisOrchestrationService(
                analysisSessionPort,
                ingestSourcesUseCase,
                cartographyUseCase,
                classifyResponsibilitiesUseCase,
                produceMigrationPlanUseCase,
                generateArtifactsUseCase,
                produceRestitutionUseCase,
                statusHistoryPort,
                workflowObservabilityPort);

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(session));
        when(analysisSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ingestSourcesUseCase.handle(any())).thenReturn(new IngestionResult(List.of(), List.of()));
        when(cartographyUseCase.handle(anyString(), anyString(), anyString())).thenReturn(cartography);
        when(classifyResponsibilitiesUseCase.handle(anyString(), anyString())).thenReturn(classification);
        when(produceMigrationPlanUseCase.handle(anyString(), anyString())).thenReturn(migrationPlan);
        when(generateArtifactsUseCase.handle(anyString(), anyString())).thenReturn(generationResult);
        when(produceRestitutionUseCase.handle(anyString(), anyString())).thenReturn(restitutionReport);

        service.orchestrate(sessionId);

        ArgumentCaptor<String> stageCaptor = ArgumentCaptor.forClass(String.class);
        verify(workflowObservabilityPort).recordPipelineOutcome(eq("success"), any());
        verify(workflowObservabilityPort, org.mockito.Mockito.times(6))
                .recordPipelineStage(stageCaptor.capture(), eq("success"), any());
        assertThat(stageCaptor.getAllValues())
                .containsExactly("ingest", "cartography", "classification", "planning", "generation", "reporting");
    }

    @Test
    void orchestrate_recordsFailure_whenSessionIsMissing() {
        AnalysisOrchestrationService service;
        service = new AnalysisOrchestrationService(
                analysisSessionPort,
                ingestSourcesUseCase,
                cartographyUseCase,
                classifyResponsibilitiesUseCase,
                produceMigrationPlanUseCase,
                generateArtifactsUseCase,
                produceRestitutionUseCase,
                statusHistoryPort,
                workflowObservabilityPort);

        when(analysisSessionPort.findById("missing")).thenReturn(Optional.empty());

        service.orchestrate("missing");

        verify(workflowObservabilityPort).recordPipelineOutcome(eq("not_found"), any());
    }
}
