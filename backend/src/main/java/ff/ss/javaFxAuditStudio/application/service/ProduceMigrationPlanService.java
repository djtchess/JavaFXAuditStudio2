package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.ProduceMigrationPlanUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.MigrationPlannerPort;
import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;
import ff.ss.javaFxAuditStudio.domain.migration.PlannedLot;

import java.util.List;
import java.util.Objects;

public class ProduceMigrationPlanService implements ProduceMigrationPlanUseCase {

    private final MigrationPlannerPort migrationPlannerPort;

    public ProduceMigrationPlanService(final MigrationPlannerPort migrationPlannerPort) {
        Objects.requireNonNull(migrationPlannerPort, "migrationPlannerPort must not be null");
        this.migrationPlannerPort = migrationPlannerPort;
    }

    @Override
    public MigrationPlan handle(final String controllerRef) {
        final List<PlannedLot> lots = migrationPlannerPort.plan(controllerRef);
        final boolean compilable = !lots.isEmpty();
        return new MigrationPlan(controllerRef, lots, compilable);
    }
}
