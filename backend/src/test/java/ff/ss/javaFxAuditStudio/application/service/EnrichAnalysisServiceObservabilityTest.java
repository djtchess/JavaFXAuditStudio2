package ff.ss.javaFxAuditStudio.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.WorkflowObservabilityPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;

@ExtendWith(MockitoExtension.class)
class EnrichAnalysisServiceObservabilityTest {

    @Mock
    private AnalysisSessionPort sessionPort;

    @Mock
    private AiEnrichmentPort enrichmentPort;

    @Mock
    private SanitizationPort sanitizationPort;

    @Mock
    private WorkflowObservabilityPort workflowObservabilityPort;

    @Test
    void enrich_recordsSuccessMetric_whenEnrichmentSucceeds() {
        EnrichAnalysisService service;
        AnalysisSession session;

        session = new AnalysisSession(
                "sess-1",
                "com/example/MyController.java",
                null,
                AnalysisStatus.COMPLETED,
                Instant.now());
        service = new EnrichAnalysisService(
                sessionPort,
                enrichmentPort,
                sanitizationPort,
                null,
                null,
                null,
                null,
                workflowObservabilityPort);

        when(sessionPort.findById("sess-1")).thenReturn(Optional.of(session));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(new SanitizedBundle("bundle-1", "com/example/MyController.java", "source", 1, "1.0", null));
        when(enrichmentPort.enrich(any())).thenReturn(
                new AiEnrichmentResult("req-1", false, "", Map.of(), 12, LlmProvider.CLAUDE_CODE));

        AiEnrichmentResult result = service.enrich("sess-1", TaskType.NAMING);

        assertThat(result.provider()).isEqualTo(LlmProvider.CLAUDE_CODE);
        verify(workflowObservabilityPort).recordLlmEnrichment(eq("claude-code"), eq("NAMING"), eq("success"), any());
    }

    @Test
    void enrich_recordsBlockedMetric_whenSanitizationIsRefused() {
        EnrichAnalysisService service;
        AnalysisSession session;

        session = new AnalysisSession(
                "sess-2",
                "com/example/SensitiveController.java",
                null,
                AnalysisStatus.COMPLETED,
                Instant.now());
        service = new EnrichAnalysisService(
                sessionPort,
                enrichmentPort,
                sanitizationPort,
                null,
                null,
                null,
                null,
                workflowObservabilityPort);

        when(sessionPort.findById("sess-2")).thenReturn(Optional.of(session));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenThrow(new SanitizationRefusedException("marker"));

        AiEnrichmentResult result = service.enrich("sess-2", TaskType.DESCRIPTION);

        assertThat(result.degraded()).isTrue();
        verify(workflowObservabilityPort).recordLlmEnrichment(eq("none"), eq("DESCRIPTION"), eq("blocked"), any());
    }
}
