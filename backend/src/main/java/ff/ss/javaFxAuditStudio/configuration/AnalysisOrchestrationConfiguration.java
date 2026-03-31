package ff.ss.javaFxAuditStudio.configuration;

import ff.ss.javaFxAuditStudio.application.ports.in.AnalysisOrchestrationUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.CartographyUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ClassifyResponsibilitiesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.IngestSourcesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceMigrationPlanUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceRestitutionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionStatusHistoryPort;
import ff.ss.javaFxAuditStudio.application.ports.out.WorkflowObservabilityPort;
import ff.ss.javaFxAuditStudio.application.service.AnalysisOrchestrationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Spring pour le service d'orchestration bout-en-bout.
 * Assemble {@link AnalysisOrchestrationService} avec toutes ses dependances.
 */
@Configuration
public class AnalysisOrchestrationConfiguration {

    @Bean
    public AnalysisOrchestrationUseCase analysisOrchestrationUseCase(
            final AnalysisSessionPort analysisSessionPort,
            final IngestSourcesUseCase ingestSourcesUseCase,
            final CartographyUseCase cartographyUseCase,
            final ClassifyResponsibilitiesUseCase classifyResponsibilitiesUseCase,
            final ProduceMigrationPlanUseCase produceMigrationPlanUseCase,
            final GenerateArtifactsUseCase generateArtifactsUseCase,
            final ProduceRestitutionUseCase produceRestitutionUseCase,
            final AnalysisSessionStatusHistoryPort statusHistoryPort,
            final WorkflowObservabilityPort observabilityPort) {
        return new AnalysisOrchestrationService(
                analysisSessionPort,
                ingestSourcesUseCase,
                cartographyUseCase,
                classifyResponsibilitiesUseCase,
                produceMigrationPlanUseCase,
                generateArtifactsUseCase,
                produceRestitutionUseCase,
                statusHistoryPort,
                observabilityPort);
    }
}
