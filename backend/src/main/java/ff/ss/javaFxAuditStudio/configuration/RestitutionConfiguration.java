package ff.ss.javaFxAuditStudio.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ff.ss.javaFxAuditStudio.application.ports.in.ProduceRestitutionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.RestitutionFormatterPort;
import ff.ss.javaFxAuditStudio.application.ports.out.RestitutionPersistencePort;
import ff.ss.javaFxAuditStudio.application.service.ProduceRestitutionService;

/**
 * Configuration Spring pour le sous-systeme de restitution.
 * Assemble le service applicatif avec le port sortant de formatage
 * et le port de persistence.
 */
@Configuration
public class RestitutionConfiguration {

    @Bean
    public ProduceRestitutionUseCase produceRestitutionUseCase(
            final RestitutionFormatterPort restitutionFormatterPort,
            final RestitutionPersistencePort restitutionPersistencePort) {
        return new ProduceRestitutionService(restitutionFormatterPort, restitutionPersistencePort);
    }
}
