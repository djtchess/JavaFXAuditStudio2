package ff.ss.javaFxAuditStudio.configuration;

import ff.ss.javaFxAuditStudio.application.ports.in.GetAnalysisSessionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.SubmitAnalysisUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionStatusHistoryPort;
import ff.ss.javaFxAuditStudio.application.service.GetAnalysisSessionService;
import ff.ss.javaFxAuditStudio.application.service.SubmitAnalysisService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du pipeline d'analyse.
 *
 * <p>Active le binding des proprietes {@code analysis.*} vers {@link AnalysisProperties}.
 */
@Configuration
@EnableConfigurationProperties(AnalysisProperties.class)
public class AnalysisConfiguration {

    @Bean
    public SubmitAnalysisUseCase submitAnalysisUseCase(
            final AnalysisSessionPort analysisSessionPort,
            final AnalysisSessionStatusHistoryPort statusHistoryPort) {
        return new SubmitAnalysisService(analysisSessionPort, statusHistoryPort);
    }

    @Bean
    public GetAnalysisSessionUseCase getAnalysisSessionUseCase(final AnalysisSessionPort analysisSessionPort) {
        return new GetAnalysisSessionService(analysisSessionPort);
    }
}
