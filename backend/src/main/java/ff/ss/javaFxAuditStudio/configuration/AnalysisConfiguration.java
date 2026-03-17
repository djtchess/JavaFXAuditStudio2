package ff.ss.javaFxAuditStudio.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du pipeline d'analyse.
 *
 * <p>Active le binding des proprietes {@code analysis.*} vers {@link AnalysisProperties}.
 */
@Configuration
@EnableConfigurationProperties(AnalysisProperties.class)
public class AnalysisConfiguration {
}
