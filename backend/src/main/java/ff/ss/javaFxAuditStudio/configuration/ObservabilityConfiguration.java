package ff.ss.javaFxAuditStudio.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;
import ff.ss.javaFxAuditStudio.adapters.out.observability.AnalysisWorkflowHealthIndicator;
import ff.ss.javaFxAuditStudio.adapters.out.observability.LlmEnrichmentHealthIndicator;
import ff.ss.javaFxAuditStudio.adapters.out.observability.WorkflowObservabilityMetricsAdapter;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;

/**
 * Assemblage des beans d'observabilite.
 */
@Configuration
public class ObservabilityConfiguration {

    @Bean
    public WorkflowObservabilityMetricsAdapter workflowObservabilityPort(
            final MeterRegistry meterRegistry,
            final AnalysisSessionPort analysisSessionPort) {
        return new WorkflowObservabilityMetricsAdapter(meterRegistry, analysisSessionPort);
    }

    @Bean
    public org.springframework.boot.health.contributor.HealthIndicator analysisWorkflowHealthIndicator(
            final AnalysisSessionPort analysisSessionPort) {
        return new AnalysisWorkflowHealthIndicator(analysisSessionPort);
    }

    @Bean
    public org.springframework.boot.health.contributor.HealthIndicator llmEnrichmentHealthIndicator(
            final AiEnrichmentProperties properties) {
        return new LlmEnrichmentHealthIndicator(properties);
    }
}
