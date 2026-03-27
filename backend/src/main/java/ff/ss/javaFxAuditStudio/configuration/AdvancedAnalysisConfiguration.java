package ff.ss.javaFxAuditStudio.configuration;

import ff.ss.javaFxAuditStudio.application.ports.in.AdvancedAnalysisUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceReaderPort;
import ff.ss.javaFxAuditStudio.application.service.AdvancedAnalysisService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdvancedAnalysisConfiguration {

    @Bean
    public AdvancedAnalysisUseCase advancedAnalysisUseCase(
            final AnalysisSessionPort analysisSessionPort,
            final SourceReaderPort sourceReaderPort,
            final RuleExtractionPort ruleExtractionPort,
            final AnalysisProperties analysisProperties) {
        return new AdvancedAnalysisService(
                analysisSessionPort,
                sourceReaderPort,
                ruleExtractionPort,
                analysisProperties);
    }
}
