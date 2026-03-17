package ff.ss.javaFxAuditStudio.configuration;

import ff.ss.javaFxAuditStudio.adapters.out.analysis.RealCodeGenerationAdapter;
import ff.ss.javaFxAuditStudio.application.ports.in.ExportArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.CodeGenerationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceReaderPort;
import ff.ss.javaFxAuditStudio.application.service.ExportArtifactsService;
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
    public GenerateArtifactsUseCase generateArtifactsUseCase(
            final CodeGenerationPort codeGenerationPort,
            final ArtifactPersistencePort artifactPersistencePort,
            final ClassificationPersistencePort classificationPersistencePort,
            final SourceReaderPort sourceReaderPort) {
        return new GenerateArtifactsService(codeGenerationPort, artifactPersistencePort,
                classificationPersistencePort, sourceReaderPort);
    }

    @Bean
    public ExportArtifactsUseCase exportArtifactsUseCase(final ArtifactPersistencePort artifactPersistencePort) {
        return new ExportArtifactsService(artifactPersistencePort);
    }
}
