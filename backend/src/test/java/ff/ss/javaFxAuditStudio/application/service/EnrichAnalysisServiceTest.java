package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de EnrichAnalysisService (JAS-017 / JAS-018).
 */
@ExtendWith(MockitoExtension.class)
class EnrichAnalysisServiceTest {

    @Mock
    private AnalysisSessionPort sessionPort;

    @Mock
    private AiEnrichmentPort enrichmentPort;

    @Mock
    private SanitizationPort sanitizationPort;

    @Mock
    private SourceFileReaderPort sourceFileReaderPort;

    private EnrichAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new EnrichAnalysisService(sessionPort, enrichmentPort, sanitizationPort);
    }

    /**
     * Bundle de test avec le controllerRef fourni — source neutre, pas de marqueur sensible.
     */
    private static SanitizedBundle bundleFor(final String controllerRef) {
        return new SanitizedBundle("test-bundle", controllerRef, "source", 1, "1.0");
    }

    @Test
    void should_throw_when_session_not_found() {
        when(sessionPort.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enrich("unknown", TaskType.NAMING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void should_delegate_to_enrichment_port_with_sanitized_bundle() {
        AnalysisSession session = new AnalysisSession(
                "sess-1", "com/example/MyController", null, AnalysisStatus.COMPLETED, Instant.now());
        when(sessionPort.findById("sess-1")).thenReturn(Optional.of(session));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(bundleFor("com/example/MyController"));

        AiEnrichmentResult expected = new AiEnrichmentResult(
                "req-1", false, "", Map.of(), 10, LlmProvider.CLAUDE_CODE);
        when(enrichmentPort.enrich(any())).thenReturn(expected);

        AiEnrichmentResult result = service.enrich("sess-1", TaskType.NAMING);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void should_pass_task_type_to_request() {
        AnalysisSession session = new AnalysisSession(
                "sess-2", "FooController", null, AnalysisStatus.COMPLETED, Instant.now());
        when(sessionPort.findById("sess-2")).thenReturn(Optional.of(session));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(bundleFor("FooController"));
        when(enrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-2", "disabled"));

        service.enrich("sess-2", TaskType.DESCRIPTION);

        ArgumentCaptor<AiEnrichmentRequest> captor = ArgumentCaptor.forClass(AiEnrichmentRequest.class);
        verify(enrichmentPort).enrich(captor.capture());
        assertThat(captor.getValue().taskType()).isEqualTo(TaskType.DESCRIPTION);
    }

    @Test
    void should_build_bundle_from_controller_name() {
        AnalysisSession session = new AnalysisSession(
                "sess-3", "MyCtrl", null, AnalysisStatus.COMPLETED, Instant.now());
        when(sessionPort.findById("sess-3")).thenReturn(Optional.of(session));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(bundleFor("MyCtrl"));
        when(enrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-3", "disabled"));

        service.enrich("sess-3", TaskType.NAMING);

        ArgumentCaptor<AiEnrichmentRequest> captor = ArgumentCaptor.forClass(AiEnrichmentRequest.class);
        verify(enrichmentPort).enrich(captor.capture());
        assertThat(captor.getValue().bundle().controllerRef()).isEqualTo("MyCtrl");
    }

    @Test
    void should_return_degraded_result_when_port_returns_degraded() {
        AnalysisSession session = new AnalysisSession(
                "sess-4", "Ctrl", null, AnalysisStatus.COMPLETED, Instant.now());
        when(sessionPort.findById("sess-4")).thenReturn(Optional.of(session));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(bundleFor("Ctrl"));
        when(enrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-4", "Circuit ouvert"));

        AiEnrichmentResult result = service.enrich("sess-4", TaskType.NAMING);

        assertThat(result.degraded()).isTrue();
        assertThat(result.degradationReason()).isEqualTo("Circuit ouvert");
    }

    @Test
    void should_reject_null_session_id() {
        assertThatThrownBy(() -> service.enrich(null, TaskType.NAMING))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_reject_null_task_type() {
        assertThatThrownBy(() -> service.enrich("sess-5", (TaskType) null))
                .isInstanceOf(NullPointerException.class);
    }

    // --- Tests JAS-018 : sanitisation ---

    @Test
    void should_return_degraded_when_sanitization_refused() {
        AnalysisSession session = new AnalysisSession(
                "sess-6", "SensitiveController", "rawSource", AnalysisStatus.COMPLETED, Instant.now());
        when(sessionPort.findById("sess-6")).thenReturn(Optional.of(session));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenThrow(new SanitizationRefusedException("marqueur sensible detecte"));

        AiEnrichmentResult result = service.enrich("sess-6", TaskType.NAMING);

        assertThat(result.degraded()).isTrue();
        assertThat(result.degradationReason()).contains("Sanitisation refusee");
        assertThat(result.provider()).isEqualTo(LlmProvider.NONE);
    }

    @Test
    void should_use_sanitized_source_in_bundle() {
        AnalysisSession session = new AnalysisSession(
                "sess-7", "MyController", "rawJavaSource", AnalysisStatus.COMPLETED, Instant.now());
        when(sessionPort.findById("sess-7")).thenReturn(Optional.of(session));

        SanitizedBundle sanitizedBundle = new SanitizedBundle(
                "bundle-id", "MyController", "sanitizedJavaSource", 10, "1.0");
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(sanitizedBundle);
        when(enrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-7", "disabled"));

        service.enrich("sess-7", TaskType.DESCRIPTION);

        ArgumentCaptor<AiEnrichmentRequest> captor = ArgumentCaptor.forClass(AiEnrichmentRequest.class);
        verify(enrichmentPort).enrich(captor.capture());
        assertThat(captor.getValue().bundle().sanitizedSource()).isEqualTo("sanitizedJavaSource");
        assertThat(captor.getValue().bundle().controllerRef()).isEqualTo("MyController");
    }

    @Test
    void should_read_raw_source_via_source_file_reader_port_when_available() {
        EnrichAnalysisService serviceWithSourceReader = new EnrichAnalysisService(
                sessionPort,
                enrichmentPort,
                sanitizationPort,
                null,
                null,
                null,
                sourceFileReaderPort);
        AnalysisSession session = new AnalysisSession(
                "sess-8", "C:/tmp/MyController.java", null, AnalysisStatus.COMPLETED, Instant.now());
        when(sessionPort.findById("sess-8")).thenReturn(Optional.of(session));
        when(sourceFileReaderPort.read("C:/tmp/MyController.java"))
                .thenReturn(Optional.of("public class MyController {}"));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(bundleFor("C:/tmp/MyController.java"));
        when(enrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-8", "disabled"));

        serviceWithSourceReader.enrich("sess-8", TaskType.NAMING);

        verify(sourceFileReaderPort).read("C:/tmp/MyController.java");
        verify(sanitizationPort).sanitize(
                any(),
                eq("public class MyController {}"),
                eq("C:/tmp/MyController.java"));
    }

    @Test
    void should_fallback_to_controller_ref_when_source_file_reader_returns_empty() {
        EnrichAnalysisService serviceWithSourceReader = new EnrichAnalysisService(
                sessionPort,
                enrichmentPort,
                sanitizationPort,
                null,
                null,
                null,
                sourceFileReaderPort);
        AnalysisSession session = new AnalysisSession(
                "sess-9", "C:/tmp/MissingController.java", null, AnalysisStatus.COMPLETED, Instant.now());
        when(sessionPort.findById("sess-9")).thenReturn(Optional.of(session));
        when(sourceFileReaderPort.read("C:/tmp/MissingController.java"))
                .thenReturn(Optional.empty());
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(bundleFor("C:/tmp/MissingController.java"));
        when(enrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-9", "disabled"));

        serviceWithSourceReader.enrich("sess-9", TaskType.NAMING);

        verify(sanitizationPort).sanitize(
                any(),
                eq("C:/tmp/MissingController.java"),
                eq("C:/tmp/MissingController.java"));
    }
}
