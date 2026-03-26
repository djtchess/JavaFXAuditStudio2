package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.adapters.out.ai.PayloadHasher;
import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.LlmAuditPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.configuration.LlmAuditProperties;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmAuditEntry;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de l'audit LLM dans EnrichAnalysisService (JAS-029).
 */
@ExtendWith(MockitoExtension.class)
class EnrichAnalysisServiceAuditTest {

    @Mock
    private AnalysisSessionPort sessionPort;

    @Mock
    private AiEnrichmentPort enrichmentPort;

    @Mock
    private SanitizationPort sanitizationPort;

    @Mock
    private LlmAuditPort auditPort;

    private PayloadHasher hasher;
    private LlmAuditProperties auditProperties;

    private EnrichAnalysisService service;

    @BeforeEach
    void setUp() {
        hasher = new PayloadHasher();
        auditProperties = new LlmAuditProperties(true);
        service = new EnrichAnalysisService(
                sessionPort,
                enrichmentPort,
                sanitizationPort,
                auditPort,
                hasher,
                auditProperties,
                null);
    }

    private static SanitizedBundle bundleFor(final String controllerRef) {
        return new SanitizedBundle("bundle-id", controllerRef, "sanitized source", 5, "1.0");
    }

    private static AnalysisSession sessionFor(final String sessionId, final String controllerName) {
        return new AnalysisSession(sessionId, controllerName, null, AnalysisStatus.COMPLETED, Instant.now());
    }

    @Test
    void should_save_audit_entry_after_nominal_enrichment() {
        when(sessionPort.findById("sess-audit-1"))
                .thenReturn(Optional.of(sessionFor("sess-audit-1", "MyController")));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(bundleFor("MyController"));
        AiEnrichmentResult result = new AiEnrichmentResult(
                "req-1", false, "", Map.of("onSave", "Save action"), 42, LlmProvider.CLAUDE_CODE);
        when(enrichmentPort.enrich(any())).thenReturn(result);

        service.enrich("sess-audit-1", TaskType.NAMING);

        ArgumentCaptor<LlmAuditEntry> captor = ArgumentCaptor.forClass(LlmAuditEntry.class);
        verify(auditPort).save(captor.capture());
        LlmAuditEntry entry = captor.getValue();
        assertThat(entry.sessionId()).isEqualTo("sess-audit-1");
        assertThat(entry.provider()).isEqualTo(LlmProvider.CLAUDE_CODE);
        assertThat(entry.taskType()).isEqualTo(TaskType.NAMING);
        assertThat(entry.degraded()).isFalse();
        assertThat(entry.sanitizationVersion()).isEqualTo("1.0");
        assertThat(entry.payloadHash()).hasSize(64);
        assertThat(entry.auditId()).isNotBlank();
        assertThat(entry.timestamp()).isNotNull();
    }

    @Test
    void should_save_degraded_audit_entry() {
        when(sessionPort.findById("sess-audit-2"))
                .thenReturn(Optional.of(sessionFor("sess-audit-2", "DegradedController")));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(bundleFor("DegradedController"));
        AiEnrichmentResult degraded = AiEnrichmentResult.degraded("req-deg", "Circuit ouvert");
        when(enrichmentPort.enrich(any())).thenReturn(degraded);

        service.enrich("sess-audit-2", TaskType.DESCRIPTION);

        ArgumentCaptor<LlmAuditEntry> captor = ArgumentCaptor.forClass(LlmAuditEntry.class);
        verify(auditPort).save(captor.capture());
        LlmAuditEntry entry = captor.getValue();
        assertThat(entry.degraded()).isTrue();
        assertThat(entry.degradationReason()).isEqualTo("Circuit ouvert");
        assertThat(entry.provider()).isEqualTo(LlmProvider.NONE);
    }

    @Test
    void should_not_fail_when_audit_port_throws() {
        when(sessionPort.findById("sess-audit-3"))
                .thenReturn(Optional.of(sessionFor("sess-audit-3", "FailController")));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(bundleFor("FailController"));
        AiEnrichmentResult result = new AiEnrichmentResult(
                "req-3", false, "", Map.of(), 10, LlmProvider.OPENAI_GPT54);
        when(enrichmentPort.enrich(any())).thenReturn(result);
        doThrow(new RuntimeException("DB error")).when(auditPort).save(any());

        // Ne doit pas lever d'exception
        AiEnrichmentResult returned = service.enrich("sess-audit-3", TaskType.NAMING);

        assertThat(returned).isEqualTo(result);
    }

    @Test
    void should_not_save_audit_when_disabled() {
        LlmAuditProperties disabledProps = new LlmAuditProperties(false);
        EnrichAnalysisService disabledService = new EnrichAnalysisService(
                sessionPort, enrichmentPort, sanitizationPort,
                auditPort, hasher, disabledProps, null);

        when(sessionPort.findById("sess-audit-4"))
                .thenReturn(Optional.of(sessionFor("sess-audit-4", "SomeController")));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(bundleFor("SomeController"));
        when(enrichmentPort.enrich(any()))
                .thenReturn(AiEnrichmentResult.degraded("req-4", "disabled"));

        disabledService.enrich("sess-audit-4", TaskType.NAMING);

        verify(auditPort, never()).save(any());
    }
}
