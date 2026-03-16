package ff.ss.javaFxAuditStudio.configuration;

import ff.ss.javaFxAuditStudio.adapters.out.analysis.RealMigrationPlannerAdapter;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceMigrationPlanUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.MigrationPlannerPort;
import ff.ss.javaFxAuditStudio.application.service.ProduceMigrationPlanService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MigrationPlanConfiguration {

    @Bean
    public MigrationPlannerPort migrationPlannerPort() {
        return new RealMigrationPlannerAdapter();
    }

    @Bean
    public ProduceMigrationPlanUseCase produceMigrationPlanUseCase(final MigrationPlannerPort migrationPlannerPort) {
        return new ProduceMigrationPlanService(migrationPlannerPort);
    }
}
