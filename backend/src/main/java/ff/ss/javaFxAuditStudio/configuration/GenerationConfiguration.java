package ff.ss.javaFxAuditStudio.configuration;

import ff.ss.javaFxAuditStudio.adapters.out.analysis.RealCodeGenerationAdapter;
import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.CodeGenerationPort;
import ff.ss.javaFxAuditStudio.application.service.GenerateArtifactsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GenerationConfiguration {

    @Bean
    public CodeGenerationPort codeGenerationPort() {
        return new RealCodeGenerationAdapter();
    }

    @Bean
    public GenerateArtifactsUseCase generateArtifactsUseCase(final CodeGenerationPort codeGenerationPort) {
        return new GenerateArtifactsService(codeGenerationPort);
    }
}
