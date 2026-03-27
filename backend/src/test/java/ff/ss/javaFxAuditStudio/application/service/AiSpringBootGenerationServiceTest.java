package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de AiSpringBootGenerationService (JAS-031).
 */
@ExtendWith(MockitoExtension.class)
class AiSpringBootGenerationServiceTest {

    @Mock
    private AnalysisSessionPort sessionPort;

    @Mock
    private ClassificationPersistencePort classificationPort;

    @Mock
    private AiEnrichmentPort aiEnrichmentPort;

    @Mock
    private SanitizationPort sanitizationPort;

    @Mock
    private SourceFileReaderPort sourceFileReaderPort;

    private AiSpringBootGenerationService service;

    @BeforeEach
    void setUp() {
        service = new AiSpringBootGenerationService(
                sessionPort, classificationPort, aiEnrichmentPort, sanitizationPort);
    }

    // --- Helpers ---

    private static AnalysisSession session(final String id, final String controllerName) {
        return new AnalysisSession(id, controllerName, null, AnalysisStatus.COMPLETED, Instant.now());
    }

    private static SanitizedBundle bundle(final String controllerRef) {
        return new SanitizedBundle("bundle-id", controllerRef, "sanitized source", 10, "1.0", null);
    }

    private static ClassificationResult classificationWithRules() {
        BusinessRule rule = new BusinessRule(
                "RG-001", "Sauvegarder le patient", "MyController.java", 42,
                ResponsibilityClass.APPLICATION, ExtractionCandidate.USE_CASE, false);
        return new ClassificationResult("MyController", List.of(rule), List.of());
    }

    // --- Tests de base ---

    @Test
    void should_throw_when_session_not_found() {
        when(sessionPort.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void should_reject_null_session_id() {
        assertThatThrownBy(() -> service.generate(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_return_degraded_when_no_classification() {
        when(sessionPort.findById("sess-1")).thenReturn(Optional.of(session("sess-1", "MyController")));
        when(classificationPort.findBySessionId("sess-1")).thenReturn(Optional.empty());

        AiCodeGenerationResult result = service.generate("sess-1");

        assertThat(result.degraded()).isTrue();
        assertThat(result.degradationReason()).contains("classification");
        assertThat(result.generatedClasses()).isEmpty();
    }

    @Test
    void should_return_degraded_when_sanitization_refused() {
        when(sessionPort.findById("sess-2")).thenReturn(Optional.of(session("sess-2", "MyController")));
        when(classificationPort.findBySessionId("sess-2"))
                .thenReturn(Optional.of(classificationWithRules()));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenThrow(new SanitizationRefusedException("marqueur sensible détecté"));

        AiCodeGenerationResult result = service.generate("sess-2");

        assertThat(result.degraded()).isTrue();
        assertThat(result.degradationReason()).contains("Sanitisation refusée");
    }

    @Test
    void should_delegate_to_ai_port_with_correct_task_type() {
        when(sessionPort.findById("sess-3")).thenReturn(Optional.of(session("sess-3", "MyController")));
        when(classificationPort.findBySessionId("sess-3"))
                .thenReturn(Optional.of(classificationWithRules()));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundle("MyController"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-3", "disabled"));

        service.generate("sess-3");

        ArgumentCaptor<AiEnrichmentRequest> captor = ArgumentCaptor.forClass(AiEnrichmentRequest.class);
        verify(aiEnrichmentPort).enrich(captor.capture());
        assertThat(captor.getValue().taskType()).isEqualTo(TaskType.SPRING_BOOT_GENERATION);
        assertThat(captor.getValue().promptTemplate()).isEqualTo("spring-boot-generation");
    }

    @Test
    void should_include_classified_rules_in_request_context() {
        when(sessionPort.findById("sess-4")).thenReturn(Optional.of(session("sess-4", "MyController")));
        when(classificationPort.findBySessionId("sess-4"))
                .thenReturn(Optional.of(classificationWithRules()));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundle("MyController"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-4", "disabled"));

        service.generate("sess-4");

        ArgumentCaptor<AiEnrichmentRequest> captor = ArgumentCaptor.forClass(AiEnrichmentRequest.class);
        verify(aiEnrichmentPort).enrich(captor.capture());
        assertThat(captor.getValue().extraContext()).containsKey("classifiedRules");
        assertThat((String) captor.getValue().extraContext().get("classifiedRules"))
                .contains("RG-001")
                .contains("USE_CASE");
    }

    @Test
    void should_return_generated_classes_on_nominal_result() {
        Map<String, String> llmSuggestions = Map.of(
                "USE_CASE", "public interface PatientUseCase { void save(); }",
                "VIEW_MODEL", "@Component\npublic class PatientViewModel {}");

        when(sessionPort.findById("sess-5")).thenReturn(Optional.of(session("sess-5", "MyController")));
        when(classificationPort.findBySessionId("sess-5"))
                .thenReturn(Optional.of(classificationWithRules()));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundle("MyController"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                new AiEnrichmentResult("req-5", false, "", llmSuggestions, 150, LlmProvider.CLAUDE_CODE));

        AiCodeGenerationResult result = service.generate("sess-5");

        assertThat(result.degraded()).isFalse();
        assertThat(result.generatedClasses()).containsKey("USE_CASE");
        assertThat(result.generatedClasses()).containsKey("VIEW_MODEL");
        assertThat(result.generatedClasses().get("USE_CASE")).contains("PatientUseCase");
        assertThat(result.tokensUsed()).isEqualTo(150);
        assertThat(result.provider()).isEqualTo(LlmProvider.CLAUDE_CODE);
    }

    @Test
    void should_return_degraded_when_llm_degraded() {
        when(sessionPort.findById("sess-6")).thenReturn(Optional.of(session("sess-6", "MyController")));
        when(classificationPort.findBySessionId("sess-6"))
                .thenReturn(Optional.of(classificationWithRules()));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundle("MyController"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-6", "Circuit ouvert"));

        AiCodeGenerationResult result = service.generate("sess-6");

        assertThat(result.degraded()).isTrue();
        assertThat(result.degradationReason()).isEqualTo("Circuit ouvert");
        assertThat(result.generatedClasses()).isEmpty();
    }

    @Test
    void should_normalize_crlf_in_generated_code() {
        Map<String, String> llmSuggestions = Map.of(
                "USE_CASE", "public interface Foo {\r\n    void bar();\r\n}");

        when(sessionPort.findById("sess-7")).thenReturn(Optional.of(session("sess-7", "MyController")));
        when(classificationPort.findBySessionId("sess-7"))
                .thenReturn(Optional.of(classificationWithRules()));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundle("MyController"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                new AiEnrichmentResult("req-7", false, "", llmSuggestions, 50, LlmProvider.OPENAI_GPT54));

        AiCodeGenerationResult result = service.generate("sess-7");

        assertThat(result.generatedClasses().get("USE_CASE")).doesNotContain("\r\n");
        assertThat(result.generatedClasses().get("USE_CASE")).contains("\n");
    }

    @Test
    void should_ignore_unsupported_artifact_types_returned_by_llm() {
        Map<String, String> llmSuggestions = Map.of(
                "USE_CASE", "public interface Foo { void bar(); }",
                "surprise", "public class ShouldBeIgnored {}");

        when(sessionPort.findById("sess-7b")).thenReturn(Optional.of(session("sess-7b", "MyController")));
        when(classificationPort.findBySessionId("sess-7b"))
                .thenReturn(Optional.of(classificationWithRules()));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundle("MyController"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                new AiEnrichmentResult("req-7b", false, "", llmSuggestions, 50, LlmProvider.OPENAI_GPT54));

        AiCodeGenerationResult result = service.generate("sess-7b");

        assertThat(result.degraded()).isFalse();
        assertThat(result.generatedClasses()).containsOnlyKeys("USE_CASE");
    }

    @Test
    void should_use_sanitized_bundle_as_source() {
        when(sessionPort.findById("sess-8")).thenReturn(Optional.of(session("sess-8", "PatientController")));
        when(classificationPort.findBySessionId("sess-8"))
                .thenReturn(Optional.of(classificationWithRules()));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(new SanitizedBundle("b-id", "PatientController", "sanitized java code", 20, "1.0", null));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-8", "disabled"));

        service.generate("sess-8");

        ArgumentCaptor<AiEnrichmentRequest> captor = ArgumentCaptor.forClass(AiEnrichmentRequest.class);
        verify(aiEnrichmentPort).enrich(captor.capture());
        assertThat(captor.getValue().bundle().sanitizedSource()).isEqualTo("sanitized java code");
        assertThat(captor.getValue().bundle().controllerRef()).isEqualTo("PatientController");
    }

    @Test
    void should_work_without_sanitization_port() {
        service = new AiSpringBootGenerationService(
                sessionPort, classificationPort, aiEnrichmentPort, null);

        when(sessionPort.findById("sess-9")).thenReturn(Optional.of(session("sess-9", "MyController")));
        when(classificationPort.findBySessionId("sess-9"))
                .thenReturn(Optional.of(classificationWithRules()));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-9", "disabled"));

        AiCodeGenerationResult result = service.generate("sess-9");

        assertThat(result).isNotNull();
    }

    @Test
    void should_read_raw_source_via_source_file_reader_port_when_available() {
        AiSpringBootGenerationService serviceWithSourceReader = new AiSpringBootGenerationService(
                sessionPort,
                classificationPort,
                aiEnrichmentPort,
                sanitizationPort,
                sourceFileReaderPort);

        when(sessionPort.findById("sess-10")).thenReturn(Optional.of(session("sess-10", "C:/tmp/MyController.java")));
        when(classificationPort.findBySessionId("sess-10"))
                .thenReturn(Optional.of(classificationWithRules()));
        when(sourceFileReaderPort.read("C:/tmp/MyController.java"))
                .thenReturn(Optional.of("public class MyController {}"));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(bundle("C:/tmp/MyController.java"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-10", "disabled"));

        serviceWithSourceReader.generate("sess-10");

        verify(sourceFileReaderPort).read("C:/tmp/MyController.java");
        verify(sanitizationPort).sanitize(
                any(),
                eq("public class MyController {}"),
                eq("C:/tmp/MyController.java"));
    }

    @Test
    void should_fallback_to_controller_ref_when_source_file_reader_returns_empty() {
        AiSpringBootGenerationService serviceWithSourceReader = new AiSpringBootGenerationService(
                sessionPort,
                classificationPort,
                aiEnrichmentPort,
                sanitizationPort,
                sourceFileReaderPort);

        when(sessionPort.findById("sess-11")).thenReturn(Optional.of(session("sess-11", "C:/tmp/MissingController.java")));
        when(classificationPort.findBySessionId("sess-11"))
                .thenReturn(Optional.of(classificationWithRules()));
        when(sourceFileReaderPort.read("C:/tmp/MissingController.java"))
                .thenReturn(Optional.empty());
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(bundle("C:/tmp/MissingController.java"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-11", "disabled"));

        serviceWithSourceReader.generate("sess-11");

        verify(sanitizationPort).sanitize(
                any(),
                eq("C:/tmp/MissingController.java"),
                eq("C:/tmp/MissingController.java"));
    }
}
