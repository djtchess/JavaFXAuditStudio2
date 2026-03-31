package ff.ss.javaFxAuditStudio.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ff.ss.javaFxAuditStudio.application.ports.in.ProduceRestitutionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.MigrationPlanPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.RestitutionFormatterPort;
import ff.ss.javaFxAuditStudio.application.ports.out.RestitutionPersistencePort;
import ff.ss.javaFxAuditStudio.application.service.ProduceRestitutionService;

@Configuration
public class RestitutionConfiguration {

    @Bean
    public ProduceRestitutionUseCase produceRestitutionUseCase(
            final RestitutionPersistencePort restitutionPersistencePort,
            final ClassificationPersistencePort classificationPersistencePort,
            final ArtifactPersistencePort artifactPersistencePort,
            final CartographyPersistencePort cartographyPersistencePort,
            final MigrationPlanPersistencePort migrationPlanPersistencePort,
            final RestitutionFormatterPort restitutionFormatterPort) {
        return new ProduceRestitutionService(
                restitutionPersistencePort,
                classificationPersistencePort,
                artifactPersistencePort,
                cartographyPersistencePort,
                migrationPlanPersistencePort,
                restitutionFormatterPort);
    }
}
