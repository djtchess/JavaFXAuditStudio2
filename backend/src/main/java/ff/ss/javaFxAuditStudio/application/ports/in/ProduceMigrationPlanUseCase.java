package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;

public interface ProduceMigrationPlanUseCase {

    MigrationPlan handle(String controllerRef);
}
