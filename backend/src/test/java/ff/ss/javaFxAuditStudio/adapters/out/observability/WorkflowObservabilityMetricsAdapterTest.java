package ff.ss.javaFxAuditStudio.adapters.out.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.micrometer.core.instrument.Counter;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class WorkflowObservabilityMetricsAdapterTest {

    @Test
    void bindTo_registersSessionGauges_and_recordsPipelineMetrics() {
        SimpleMeterRegistry registry;
        AnalysisSessionPort sessionPort;
        WorkflowObservabilityMetricsAdapter adapter;

        registry = new SimpleMeterRegistry();
        sessionPort = Mockito.mock(AnalysisSessionPort.class);
        when(sessionPort.findAll()).thenReturn(List.of(
                session("s1", AnalysisStatus.CREATED),
                session("s2", AnalysisStatus.COMPLETED),
                session("s3", AnalysisStatus.FAILED),
                session("s4", AnalysisStatus.INGESTING)));
        adapter = new WorkflowObservabilityMetricsAdapter(registry, sessionPort);

        adapter.bindTo(registry);

        assertThat(registry.find("jas.analysis.sessions").tag("status", "total").gauge()).isNotNull();
        assertThat(registry.find("jas.analysis.sessions").tag("status", "created").gauge().value()).isEqualTo(1.0);
        assertThat(registry.find("jas.analysis.sessions").tag("status", "completed").gauge().value()).isEqualTo(1.0);
        assertThat(registry.find("jas.analysis.sessions").tag("status", "failed").gauge().value()).isEqualTo(1.0);

        adapter.recordPipelineStage("ingest", "success", Duration.ofMillis(42));
        adapter.recordPipelineOutcome("success", Duration.ofMillis(180));
        adapter.recordLlmEnrichment("claude-code", "NAMING", "success", Duration.ofMillis(95));

        Counter stageCounter = registry.find("jas.analysis.pipeline.stage.count")
                .tag("stage", "ingest")
                .tag("outcome", "success")
                .counter();
        Counter pipelineCounter = registry.find("jas.analysis.pipeline.count")
                .tag("outcome", "success")
                .counter();
        Counter llmCounter = registry.find("jas.llm.enrichment.count")
                .tag("provider", "claude-code")
                .tag("taskType", "naming")
                .tag("outcome", "success")
                .counter();

        assertThat(stageCounter.count()).isEqualTo(1.0);
        assertThat(pipelineCounter.count()).isEqualTo(1.0);
        assertThat(llmCounter.count()).isEqualTo(1.0);
        assertThat(registry.find("jas.analysis.pipeline.stage.duration")
                .tag("stage", "ingest")
                .tag("outcome", "success")
                .timer().count()).isEqualTo(1L);
        assertThat(registry.find("jas.llm.enrichment.duration")
                .tag("provider", "claude-code")
                .tag("taskType", "naming")
                .tag("outcome", "success")
                .timer().count()).isEqualTo(1L);
    }

    private static AnalysisSession session(final String sessionId, final AnalysisStatus status) {
        return new AnalysisSession(sessionId, "Controller.java", null, status, Instant.now());
    }
}
