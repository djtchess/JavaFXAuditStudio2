package ff.ss.javaFxAuditStudio.adapters.out.observability;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.WorkflowObservabilityPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;

/**
 * Adapter Micrometer qui enregistre les compteurs, timers et jauges du moteur.
 */
public final class WorkflowObservabilityMetricsAdapter implements WorkflowObservabilityPort, MeterBinder {

    private static final String PIPELINE_PREFIX = "jas.analysis.pipeline";
    private static final String LLM_PREFIX = "jas.llm.enrichment";
    private static final String SESSIONS_METRIC = "jas.analysis.sessions";

    private final MeterRegistry meterRegistry;
    private final AnalysisSessionPort analysisSessionPort;

    public WorkflowObservabilityMetricsAdapter(
            final MeterRegistry meterRegistry,
            final AnalysisSessionPort analysisSessionPort) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
        this.analysisSessionPort = Objects.requireNonNull(analysisSessionPort, "analysisSessionPort must not be null");
    }

    @Override
    public void bindTo(final MeterRegistry registry) {
        registerSessionGauge(registry, "total", sessions -> sessions.length);
        for (AnalysisStatus status : AnalysisStatus.values()) {
            registerSessionGauge(registry, status.name().toLowerCase(), sessions -> countSessions(sessions, status));
        }
    }

    @Override
    public void recordPipelineStage(
            final String stage,
            final String outcome,
            final Duration duration) {
        recordTimedMetric(
                PIPELINE_PREFIX + ".stage",
                duration,
                "stage", normalizeTag(stage),
                "outcome", normalizeTag(outcome));
    }

    @Override
    public void recordPipelineOutcome(final String outcome, final Duration duration) {
        recordTimedMetric(
                PIPELINE_PREFIX,
                duration,
                "outcome", normalizeTag(outcome));
    }

    @Override
    public void recordLlmEnrichment(
            final String provider,
            final String taskType,
            final String outcome,
            final Duration duration) {
        recordTimedMetric(
                LLM_PREFIX,
                duration,
                "provider", normalizeTag(provider),
                "taskType", normalizeTag(taskType),
                "outcome", normalizeTag(outcome));
    }

    private void registerSessionGauge(
            final MeterRegistry registry,
            final String statusTag,
            final SessionCountFunction countFunction) {
        Gauge.builder(SESSIONS_METRIC, this, adapter -> countFunction.count(adapter.snapshotSessions()))
                .description("Nombre de sessions d'analyse par statut")
                .tag("status", statusTag)
                .register(registry);
    }

    private void recordTimedMetric(
            final String metricPrefix,
            final Duration duration,
            final String... tags) {
        Duration safeDuration = (duration != null) ? duration : Duration.ZERO;
        Timer.builder(metricPrefix + ".duration")
                .tags(tags)
                .register(meterRegistry)
                .record(safeDuration);
        Counter.builder(metricPrefix + ".count")
                .tags(tags)
                .register(meterRegistry)
                .increment();
    }

    private AnalysisSession[] snapshotSessions() {
        return analysisSessionPort.findAll().toArray(AnalysisSession[]::new);
    }

    private static long countSessions(final AnalysisSession[] sessions, final AnalysisStatus status) {
        return Arrays.stream(sessions)
                .filter(session -> session.status() == status)
                .count();
    }

    private static String normalizeTag(final String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase();
    }

    @FunctionalInterface
    private interface SessionCountFunction {
        long count(AnalysisSession[] sessions);
    }
}
