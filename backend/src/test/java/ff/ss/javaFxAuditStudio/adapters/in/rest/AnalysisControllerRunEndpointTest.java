package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AnalysisSessionResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.OrchestratedAnalysisResultResponse;
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
import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceMigrationPlanUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceRestitutionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import ff.ss.javaFxAuditStudio.domain.workbench.OrchestratedAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisControllerRunEndpointTest {

    @Mock
    private CartographyUseCase cartographyUseCase;

    @Mock
    private ClassifyResponsibilitiesUseCase classifyResponsibilitiesUseCase;

    @Mock
    private GenerateArtifactsUseCase generateArtifactsUseCase;

    @Mock
    private ProduceMigrationPlanUseCase produceMigrationPlanUseCase;

    @Mock
    private ProduceRestitutionUseCase produceRestitutionUseCase;

    @Mock
    private AnalysisSessionPort analysisSessionPort;

    @Mock
    private AnalysisOrchestrationUseCase analysisOrchestrationUseCase;

    @Mock
    private AnalysisSessionResponseMapper analysisSessionResponseMapper;

    @Mock
    private CartographyResponseMapper cartographyResponseMapper;

    @Mock
    private ClassificationResponseMapper classificationResponseMapper;

    @Mock
    private MigrationPlanResponseMapper migrationPlanResponseMapper;

    @Mock
    private ArtifactsResponseMapper artifactsResponseMapper;

    @Mock
    private RestitutionReportResponseMapper restitutionReportResponseMapper;

    @Mock
    private OrchestratedAnalysisResultResponseMapper orchestratedAnalysisResultResponseMapper;

    @Mock
    private ExportArtifactsUseCase exportArtifactsUseCase;

    private AnalysisController controller;

    @BeforeEach
    void setUp() {
        controller = new AnalysisController(
                cartographyUseCase,
                classifyResponsibilitiesUseCase,
                generateArtifactsUseCase,
                produceMigrationPlanUseCase,
                produceRestitutionUseCase,
                analysisSessionPort,
                analysisOrchestrationUseCase,
                analysisSessionResponseMapper,
                cartographyResponseMapper,
                classificationResponseMapper,
                migrationPlanResponseMapper,
                artifactsResponseMapper,
                restitutionReportResponseMapper,
                orchestratedAnalysisResultResponseMapper,
                exportArtifactsUseCase);
    }

    @Test
    void orchestrate_returns404_whenSessionNotFound() {
        String sessionId;
        ResponseEntity<OrchestratedAnalysisResultResponse> response;

        sessionId = "session-introuvable";
        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.empty());

        response = controller.runPipeline(sessionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void orchestrate_returns409_whenStatusNotCreated() {
        String sessionId;
        AnalysisSession sessionEnCours;
        ResponseEntity<OrchestratedAnalysisResultResponse> response;

        sessionId = "session-en-cours";
        sessionEnCours = new AnalysisSession(
                sessionId,
                "com/example/MyController.java",
                null,
                AnalysisStatus.IN_PROGRESS,
                Instant.now());

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(sessionEnCours));

        response = controller.runPipeline(sessionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void orchestrate_returns200_whenStatusCreated() {
        String sessionId;
        AnalysisSession sessionCreee;
        OrchestratedAnalysisResult orchestrationResult;
        OrchestratedAnalysisResultResponse responseDto;
        ResponseEntity<OrchestratedAnalysisResultResponse> response;

        sessionId = "session-prete";
        sessionCreee = new AnalysisSession(
                sessionId,
                "com/example/MyController.java",
                null,
                AnalysisStatus.CREATED,
                Instant.now());

        orchestrationResult = new OrchestratedAnalysisResult(
                sessionId,
                AnalysisStatus.COMPLETED,
                null, null, null, null, null,
                List.of());

        responseDto = new OrchestratedAnalysisResultResponse(
                sessionId,
                "COMPLETED",
                null, null, null, null, null,
                List.of());

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(sessionCreee));
        when(analysisOrchestrationUseCase.orchestrate(sessionId)).thenReturn(orchestrationResult);
        when(orchestratedAnalysisResultResponseMapper.toResponse(orchestrationResult)).thenReturn(responseDto);

        response = controller.runPipeline(sessionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().sessionId()).isEqualTo(sessionId);
        assertThat(response.getBody().finalStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void getSession_returns200_whenSessionExists() {
        String sessionId;
        AnalysisSession session;
        AnalysisSessionResponse responseDto;
        ResponseEntity<AnalysisSessionResponse> response;

        sessionId = "session-detaillee";
        session = new AnalysisSession(
                sessionId,
                "com/example/MyController.java",
                "snippets/MyController.txt",
                AnalysisStatus.CARTOGRAPHING,
                Instant.now());

        responseDto = new AnalysisSessionResponse(
                sessionId,
                "CARTOGRAPHING",
                "com/example/MyController.java",
                "snippets/MyController.txt");

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(session));
        when(analysisSessionResponseMapper.toResponse(session)).thenReturn(responseDto);

        response = controller.getSession(sessionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().sessionId()).isEqualTo(sessionId);
        assertThat(response.getBody().status()).isEqualTo("CARTOGRAPHING");
    }

    @Test
    void getSession_returns404_whenSessionMissing() {
        String sessionId;
        ResponseEntity<AnalysisSessionResponse> response;

        sessionId = "session-absente";
        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.empty());

        response = controller.getSession(sessionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }
}
