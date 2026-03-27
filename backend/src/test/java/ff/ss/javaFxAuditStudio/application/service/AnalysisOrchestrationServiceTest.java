package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.CartographyUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ClassifyResponsibilitiesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.IngestSourcesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceMigrationPlanUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceRestitutionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
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
import ff.ss.javaFxAuditStudio.domain.workbench.OrchestratedAnalysisResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnalysisOrchestrationServiceTest {

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
    private WorkflowObservabilityPort workflowObservabilityPort;

    @InjectMocks
    private AnalysisOrchestrationService service;

    @Test
    void orchestrate_returnsFailed_whenSessionNotFound() {
        String sessionId;
        OrchestratedAnalysisResult result;

        sessionId = "session-absent";
        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.empty());

        result = service.orchestrate(sessionId);

        assertThat(result.finalStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(result.errors()).isNotEmpty();
        assertThat(result.sessionId()).isEqualTo(sessionId);
    }

    @Test
    void orchestrate_returnsCompleted_whenPipelineSucceeds() {
        String sessionId;
        AnalysisSession session;
        IngestionResult ingestionResult;
        ControllerCartography cartography;
        ClassificationResult classification;
        MigrationPlan migrationPlan;
        GenerationResult generationResult;
        RestitutionSummary restitutionSummary;
        RestitutionReport restitutionReport;
        OrchestratedAnalysisResult result;

        sessionId = "session-ok";
        session = new AnalysisSession(
                sessionId,
                "com/example/MyController.java",
                "com/example/MyController.java",
                AnalysisStatus.CREATED,
                Instant.now());

        ingestionResult = new IngestionResult(
                List.of(new SourceInput(
                        "com/example/MyController.java",
                        SourceInputType.JAVA_CONTROLLER,
                        "class MyController {}")),
                List.of());

        cartography = new ControllerCartography(
                "com/example/MyController.java",
                "",
                List.of(),
                List.of(),
                List.of());

        classification = new ClassificationResult(
                "com/example/MyController.java",
                List.of(),
                List.of());

        migrationPlan = new MigrationPlan(
                "com/example/MyController.java",
                List.of(),
                true);

        generationResult = new GenerationResult(
                "com/example/MyController.java",
                List.of(),
                List.of());

        restitutionSummary = new RestitutionSummary(
                "com/example/MyController.java",
                0, 0, 0, 0,
                ConfidenceLevel.HIGH,
                false);
        restitutionReport = new RestitutionReport(
                restitutionSummary,
                List.of(),
                List.of(),
                List.of());

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(session));
        when(analysisSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ingestSourcesUseCase.handle(any())).thenReturn(ingestionResult);
        when(cartographyUseCase.handle(anyString(), anyString(), anyString())).thenReturn(cartography);
        when(classifyResponsibilitiesUseCase.handle(anyString(), anyString())).thenReturn(classification);
        when(produceMigrationPlanUseCase.handle(anyString(), anyString())).thenReturn(migrationPlan);
        when(generateArtifactsUseCase.handle(anyString(), anyString())).thenReturn(generationResult);
        when(produceRestitutionUseCase.handle(anyString(), anyString())).thenReturn(restitutionReport);

        result = service.orchestrate(sessionId);

        assertThat(result.finalStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(result.errors()).isEmpty();
        assertThat(result.sessionId()).isEqualTo(sessionId);
        assertThat(result.cartography()).isNotNull();
        assertThat(result.classification()).isNotNull();
        assertThat(result.migrationPlan()).isNotNull();
        assertThat(result.generationResult()).isNotNull();
        assertThat(result.restitutionReport()).isNotNull();

        ArgumentCaptor<AnalysisSession> captor = ArgumentCaptor.forClass(AnalysisSession.class);
        verify(analysisSessionPort, times(8)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(AnalysisSession::status)
                .containsExactly(
                        AnalysisStatus.IN_PROGRESS,
                        AnalysisStatus.INGESTING,
                        AnalysisStatus.CARTOGRAPHING,
                        AnalysisStatus.CLASSIFYING,
                        AnalysisStatus.PLANNING,
                        AnalysisStatus.GENERATING,
                        AnalysisStatus.REPORTING,
                        AnalysisStatus.COMPLETED);
    }

    @Test
    void orchestrate_returnsFailed_whenUseCaseThrowsRuntimeException() {
        String sessionId;
        AnalysisSession session;
        String errorMessage;
        OrchestratedAnalysisResult result;

        sessionId = "session-erreur";
        errorMessage = "ingestion impossible : fichier corrompu";
        session = new AnalysisSession(
                sessionId,
                "com/example/BrokenController.java",
                null,
                AnalysisStatus.CREATED,
                Instant.now());

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(session));
        when(analysisSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ingestSourcesUseCase.handle(any()))
                .thenThrow(new RuntimeException(errorMessage));

        result = service.orchestrate(sessionId);

        assertThat(result.finalStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0)).isEqualTo(errorMessage);
        assertThat(result.sessionId()).isEqualTo(sessionId);

        ArgumentCaptor<AnalysisSession> captor = ArgumentCaptor.forClass(AnalysisSession.class);
        verify(analysisSessionPort, times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(AnalysisSession::status)
                .containsExactly(
                        AnalysisStatus.IN_PROGRESS,
                        AnalysisStatus.INGESTING,
                        AnalysisStatus.FAILED);
    }
}
