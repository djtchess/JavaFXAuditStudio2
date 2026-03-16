package ff.ss.javaFxAuditStudio.configuration;

import ff.ss.javaFxAuditStudio.adapters.out.analysis.JavaControllerRuleExtractionAdapter;
import ff.ss.javaFxAuditStudio.application.ports.in.ClassifyResponsibilitiesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.application.service.ClassifyResponsibilitiesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClassificationConfiguration {

    @Bean
    public RuleExtractionPort ruleExtractionPort() {
        return new JavaControllerRuleExtractionAdapter();
    }

    @Bean
    public ClassifyResponsibilitiesUseCase classifyResponsibilitiesUseCase(
            final RuleExtractionPort ruleExtractionPort) {
        ClassifyResponsibilitiesUseCase useCase;

        useCase = new ClassifyResponsibilitiesService(ruleExtractionPort);
        return useCase;
    }
}
