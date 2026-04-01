package ff.ss.javaFxAuditStudio.configuration;

import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.GetReclassificationHistoryUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ReclassifyRuleUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.application.service.GetReclassificationHistoryService;
import ff.ss.javaFxAuditStudio.application.service.ReclassifyRuleService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du service de reclassification (JAS-012 / JAS-013).
 * Assemble ReclassifyRuleService avec ses dependances de port.
 */
@Configuration
public class ReclassificationConfiguration {

    @Bean
    public ReclassifyRuleUseCase reclassifyRuleUseCase(
            final AnalysisSessionPort analysisSessionPort,
            final ClassificationPersistencePort classificationPersistencePort,
            final ReclassificationAuditPort reclassificationAuditPort,
            final GenerateArtifactsUseCase generateArtifactsUseCase) {
        return new ReclassifyRuleService(
                analysisSessionPort,
                classificationPersistencePort,
                reclassificationAuditPort,
                generateArtifactsUseCase);
    }

    @Bean
    public GetReclassificationHistoryUseCase getReclassificationHistoryUseCase(
            final ReclassificationAuditPort reclassificationAuditPort) {
        return new GetReclassificationHistoryService(reclassificationAuditPort);
    }
}
