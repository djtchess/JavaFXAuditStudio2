package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.ProduceMigrationPlanUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.MigrationPlanPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.MigrationPlannerPort;
import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;
import ff.ss.javaFxAuditStudio.domain.migration.PlannedLot;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ProduceMigrationPlanService implements ProduceMigrationPlanUseCase {

    private final MigrationPlannerPort migrationPlannerPort;
    private final MigrationPlanPersistencePort migrationPlanPersistencePort;

    public ProduceMigrationPlanService(
            final MigrationPlannerPort migrationPlannerPort,
            final MigrationPlanPersistencePort migrationPlanPersistencePort) {
        this.migrationPlannerPort = Objects.requireNonNull(
                migrationPlannerPort, "migrationPlannerPort must not be null");
        this.migrationPlanPersistencePort = Objects.requireNonNull(
                migrationPlanPersistencePort, "migrationPlanPersistencePort must not be null");
    }

    @Override
    public MigrationPlan handle(final String sessionId, final String controllerRef) {
        Optional<MigrationPlan> cached = migrationPlanPersistencePort.findBySessionId(sessionId);
        if (cached.isPresent()) {
            return cached.get();
        }

        final List<PlannedLot> lots = migrationPlannerPort.plan(controllerRef);
        final boolean compilable = !lots.isEmpty();
        MigrationPlan result = new MigrationPlan(controllerRef, lots, compilable);

        migrationPlanPersistencePort.save(sessionId, result);
        return result;
    }
}
