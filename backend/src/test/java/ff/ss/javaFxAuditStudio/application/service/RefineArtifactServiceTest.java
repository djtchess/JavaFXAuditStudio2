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
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.ArtifactRefineRequest;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.cartography.FxmlComponent;
import ff.ss.javaFxAuditStudio.domain.cartography.HandlerBinding;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import ff.ss.javaFxAuditStudio.domain.rules.ReclassificationAuditEntry;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefineArtifactServiceTest {

    @Mock
    private AnalysisSessionPort sessionPort;

    @Mock
    private ClassificationPersistencePort classificationPort;

    @Mock
    private CartographyPersistencePort cartographyPort;

    @Mock
    private ReclassificationAuditPort reclassificationAuditPort;

    @Mock
    private AiEnrichmentPort aiEnrichmentPort;

    @Mock
    private SanitizationPort sanitizationPort;

    @Mock
    private SourceFileReaderPort sourceFileReaderPort;

    private RefineArtifactService service;

    @BeforeEach
    void setUp() {
        service = new RefineArtifactService(
                sessionPort,
                classificationPort,
                cartographyPort,
                reclassificationAuditPort,
                aiEnrichmentPort,
                sanitizationPort,
                sourceFileReaderPort);
    }

    @Test
    void refine_includes_context_and_previous_code() {
        AnalysisSession session = new AnalysisSession(
                "sess-1",
                "Controller",
                "Controller.java",
                AnalysisStatus.COMPLETED,
                Instant.now());
        ClassificationResult classification = classification();
        when(sessionPort.findById("sess-1")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-1")).thenReturn(Optional.of(classification));
        when(cartographyPort.findBySessionId("sess-1")).thenReturn(Optional.of(cartography()));
        when(reclassificationAuditPort.findByAnalysisIdAndRuleId("sess-1", "RG-001"))
                .thenReturn(List.of(reclassification()));
        when(sourceFileReaderPort.read("Controller")).thenReturn(Optional.of("class Controller {}"));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(new ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle(
                        "bundle-1", "Controller", "sanitized", 10, "1.0", null));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-1", "disabled"));

        ArtifactRefineRequest request = new ArtifactRefineRequest(
                ArtifactType.USE_CASE,
                "ameliore la lisibilite",
                "public interface PatientUseCase {}");

        AiCodeGenerationResult result = service.refine("sess-1", request);

        ArgumentCaptor<AiEnrichmentRequest> captor = ArgumentCaptor.forClass(AiEnrichmentRequest.class);
        verify(aiEnrichmentPort).enrich(captor.capture());
        assertThat(captor.getValue().promptTemplate()).isEqualTo("artifact-refine");
        assertThat(captor.getValue().extraContext()).containsKeys(
                "artifactType",
                "instruction",
                "previousCode",
                "screenContext",
                "reclassificationFeedback");
        assertThat((String) captor.getValue().extraContext().get("artifactType")).isEqualTo("USE_CASE");
        assertThat(result.degraded()).isTrue();
    }

    @Test
    void refine_returns_nominal_generation_result() {
        AnalysisSession session = new AnalysisSession(
                "sess-2",
                "Controller",
                "Controller.java",
                AnalysisStatus.COMPLETED,
                Instant.now());
        ClassificationResult classification = classification();
        when(sessionPort.findById("sess-2")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-2")).thenReturn(Optional.of(classification));
        when(sourceFileReaderPort.read("Controller")).thenReturn(Optional.of("class Controller {}"));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(new ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle(
                        "bundle-2", "Controller", "sanitized", 10, "1.0", null));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                new AiEnrichmentResult(
                        "req-2",
                        false,
                        "",
                        Map.of("USE_CASE", "public interface RefinedUseCase {}"),
                        99,
                        LlmProvider.CLAUDE_CODE));

        AiCodeGenerationResult result = service.refine(
                "sess-2",
                new ArtifactRefineRequest(ArtifactType.USE_CASE, "instr", "code"));

        assertThat(result.degraded()).isFalse();
        assertThat(result.generatedClasses()).containsEntry(
                "USE_CASE", "public interface RefinedUseCase {}");
        assertThat(result.tokensUsed()).isEqualTo(99);
    }

    @Test
    void refine_throws_when_session_missing() {
        when(sessionPort.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refine(
                "unknown",
                new ArtifactRefineRequest(ArtifactType.USE_CASE, "instr", "code")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refine_degrades_when_sanitization_refused() {
        AnalysisSession session = new AnalysisSession(
                "sess-3",
                "Controller",
                "Controller.java",
                AnalysisStatus.COMPLETED,
                Instant.now());
        when(sessionPort.findById("sess-3")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-3")).thenReturn(Optional.of(classification()));
        when(sourceFileReaderPort.read("Controller")).thenReturn(Optional.of("class Controller {}"));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenThrow(new SanitizationRefusedException("secret"));

        AiCodeGenerationResult result = service.refine(
                "sess-3",
                new ArtifactRefineRequest(ArtifactType.USE_CASE, "instr", "code"));

        assertThat(result.degraded()).isTrue();
        assertThat(result.degradationReason()).contains("Sanitisation refusee");
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

    private static ReclassificationAuditEntry reclassification() {
        return new ReclassificationAuditEntry(
                "audit-1",
                "sess-1",
                "RG-001",
                ResponsibilityClass.APPLICATION,
                ResponsibilityClass.BUSINESS,
                "plus metier",
                Instant.now());
    }
}
