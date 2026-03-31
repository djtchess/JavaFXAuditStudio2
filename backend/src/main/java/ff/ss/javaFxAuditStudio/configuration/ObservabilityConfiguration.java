package ff.ss.javaFxAuditStudio.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;
import ff.ss.javaFxAuditStudio.adapters.out.ai.AiCircuitBreaker;
import ff.ss.javaFxAuditStudio.adapters.out.observability.AiHealthEndpoint;
import ff.ss.javaFxAuditStudio.adapters.out.observability.AnalysisWorkflowHealthIndicator;
import ff.ss.javaFxAuditStudio.adapters.out.observability.LlmEnrichmentHealthIndicator;
import ff.ss.javaFxAuditStudio.adapters.out.observability.WorkflowObservabilityMetricsAdapter;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionStatusHistoryPort;

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
            final AnalysisSessionPort analysisSessionPort,
            final AnalysisSessionStatusHistoryPort statusHistoryPort) {
        return new AnalysisWorkflowHealthIndicator(analysisSessionPort, statusHistoryPort);
    }

    @Bean
    public org.springframework.boot.health.contributor.HealthIndicator llmEnrichmentHealthIndicator(
            final AiEnrichmentProperties properties) {
        return new LlmEnrichmentHealthIndicator(properties);
    }

    @Bean
    public AiHealthEndpoint aiHealthEndpoint(
            final MeterRegistry meterRegistry,
            final AiEnrichmentProperties properties,
            final AiCircuitBreaker circuitBreaker) {
        return new AiHealthEndpoint(meterRegistry, properties, circuitBreaker);
    }
}
