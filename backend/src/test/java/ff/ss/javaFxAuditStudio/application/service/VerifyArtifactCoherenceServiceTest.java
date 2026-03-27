package ff.ss.javaFxAuditStudio.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.ArtifactCoherenceResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.cartography.FxmlComponent;
import ff.ss.javaFxAuditStudio.domain.cartography.HandlerBinding;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerifyArtifactCoherenceServiceTest {

    @Mock
    private AnalysisSessionPort sessionPort;

    @Mock
    private ClassificationPersistencePort classificationPort;

    @Mock
    private CartographyPersistencePort cartographyPort;

    @Mock
    private ArtifactPersistencePort artifactPersistencePort;

    @Mock
    private ReclassificationAuditPort reclassificationAuditPort;

    @Mock
    private AiEnrichmentPort aiEnrichmentPort;

    @Mock
    private SanitizationPort sanitizationPort;

    @Mock
    private SourceFileReaderPort sourceFileReaderPort;

    private VerifyArtifactCoherenceService service;

    @BeforeEach
    void setUp() {
        service = new VerifyArtifactCoherenceService(
                sessionPort,
                classificationPort,
                cartographyPort,
                artifactPersistencePort,
                reclassificationAuditPort,
                aiEnrichmentPort,
                sanitizationPort,
                sourceFileReaderPort);
    }

    @Test
    void verify_includes_generated_artifacts_in_prompt_context() {
        AnalysisSession session = new AnalysisSession(
                "sess-1",
                "Controller",
                "Controller.java",
                AnalysisStatus.COMPLETED,
                Instant.now());
        when(sessionPort.findById("sess-1")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-1")).thenReturn(Optional.of(classification()));
        when(cartographyPort.findBySessionId("sess-1")).thenReturn(Optional.of(cartography()));
        when(artifactPersistencePort.findBySessionId("sess-1")).thenReturn(Optional.of(generationResult()));
        when(sourceFileReaderPort.read("Controller")).thenReturn(Optional.of("class Controller {}"));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(new ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle(
                        "bundle-1", "Controller", "sanitized", 10, "1.0", null));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                new AiEnrichmentResult(
                        "req-1",
                        false,
                        "",
                        Map.of(
                                "coherent", "true",
                                "global", "tout est aligne",
                                "artifact_USE_CASE", "ok"),
                        77,
                        LlmProvider.OPENAI_GPT54));

        ArtifactCoherenceResult result = service.verify("sess-1");

        ArgumentCaptor<AiEnrichmentRequest> captor = ArgumentCaptor.forClass(AiEnrichmentRequest.class);
        verify(aiEnrichmentPort).enrich(captor.capture());
        assertThat(captor.getValue().promptTemplate()).isEqualTo("artifact-coherence");
        assertThat(captor.getValue().extraContext()).containsKeys(
                "generatedArtifacts",
                "screenContext",
                "reclassificationFeedback",
                "classifiedRules");
        assertThat(result.coherent()).isTrue();
        assertThat(result.artifactIssues()).containsEntry("USE_CASE", "ok");
        assertThat(result.globalSuggestions()).containsExactly("tout est aligne");
    }

    @Test
    void verify_returns_degraded_when_no_artifacts_exist() {
        AnalysisSession session = new AnalysisSession(
                "sess-2",
                "Controller",
                "Controller.java",
                AnalysisStatus.COMPLETED,
                Instant.now());
        when(sessionPort.findById("sess-2")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-2")).thenReturn(Optional.of(classification()));
        when(artifactPersistencePort.findBySessionId("sess-2")).thenReturn(Optional.empty());

        ArtifactCoherenceResult result = service.verify("sess-2");

        assertThat(result.degraded()).isTrue();
        assertThat(result.degradationReason()).contains("Aucun artefact genere");
    }

    @Test
    void verify_throws_when_session_missing() {
        when(sessionPort.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ClassificationResult classification() {
        BusinessRule rule = new BusinessRule(
                "RG-001",
                "Orchestration du use case",
                "Controller.java",
                12,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false);
        return new ClassificationResult("Controller", List.of(rule), List.of());
    }

    private static ControllerCartography cartography() {
        return new ControllerCartography(
                "Controller",
                "controller.fxml",
                List.of(new FxmlComponent("saveButton", "Button", "onSave")),
                List.of(new HandlerBinding("onSave", "#saveButton", "Button")),
                List.of());
    }

    private static GenerationResult generationResult() {
        return new GenerationResult(
                "Controller",
                List.of(new CodeArtifact(
                        "art-1",
                        ArtifactType.USE_CASE,
                        1,
                        "PatientUseCase",
                        "public interface PatientUseCase {}",
                        false)),
                List.of());
    }
}
