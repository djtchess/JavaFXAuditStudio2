package ff.ss.javaFxAuditStudio.configuration;

import ff.ss.javaFxAuditStudio.adapters.out.analysis.JavaControllerRuleExtractionAdapter;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.JavaParserRuleExtractionAdapter;
import ff.ss.javaFxAuditStudio.application.ports.in.ClassifyResponsibilitiesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceReaderPort;
import ff.ss.javaFxAuditStudio.application.service.ClassifyResponsibilitiesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class ClassificationConfiguration {

    @Bean
    public RuleExtractionPort ruleExtractionPort(final AnalysisProperties analysisProperties) {
        Set<String> lifecycleExcluded = analysisProperties.lifecycleMethods() != null
                ? analysisProperties.lifecycleMethods().asSet()
                : Set.of();
        AnalysisProperties.ClassificationPatterns classificationPatterns =
                analysisProperties.classificationPatterns() != null
                        ? analysisProperties.classificationPatterns()
                        : new AnalysisProperties.ClassificationPatterns(
                                null, null, null, null, null, null, null, null);
        RuleExtractionPort regexFallback = new JavaControllerRuleExtractionAdapter(
                lifecycleExcluded, classificationPatterns);
        return new JavaParserRuleExtractionAdapter(regexFallback, lifecycleExcluded, classificationPatterns);
    }

    @Bean
    public ClassifyResponsibilitiesUseCase classifyResponsibilitiesUseCase(
            final RuleExtractionPort ruleExtractionPort,
            final ClassificationPersistencePort classificationPersistencePort,
            final SourceReaderPort sourceReaderPort) {
        return new ClassifyResponsibilitiesService(ruleExtractionPort, classificationPersistencePort, sourceReaderPort);
    }
}
