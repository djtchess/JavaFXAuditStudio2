package ff.ss.javaFxAuditStudio.application.ports.out;

import java.time.Duration;

/**
 * Port sortant pour l'observabilite des workflows metiers.
 */
public interface WorkflowObservabilityPort {

    void recordPipelineStage(String stage, String outcome, Duration duration);

    void recordPipelineOutcome(String outcome, Duration duration);

    void recordLlmEnrichment(String provider, String taskType, String outcome, Duration duration);

    static WorkflowObservabilityPort noop() {
        return new WorkflowObservabilityPort() {
            @Override
            public void recordPipelineStage(final String stage, final String outcome, final Duration duration) {
                // no-op
            }

            @Override
            public void recordPipelineOutcome(final String outcome, final Duration duration) {
                // no-op
            }

            @Override
            public void recordLlmEnrichment(
                    final String provider,
                    final String taskType,
                    final String outcome,
                    final Duration duration) {
                // no-op
            }
        };
    }
}
