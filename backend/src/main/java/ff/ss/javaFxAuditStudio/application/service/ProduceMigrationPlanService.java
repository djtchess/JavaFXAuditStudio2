package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.ProduceMigrationPlanUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
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
    private final CartographyPersistencePort cartographyPersistencePort;
    private final ClassificationPersistencePort classificationPersistencePort;

    public ProduceMigrationPlanService(
            final MigrationPlannerPort migrationPlannerPort,
            final MigrationPlanPersistencePort migrationPlanPersistencePort,
            final CartographyPersistencePort cartographyPersistencePort,
            final ClassificationPersistencePort classificationPersistencePort) {
        this.migrationPlannerPort = Objects.requireNonNull(migrationPlannerPort);
        this.migrationPlanPersistencePort = Objects.requireNonNull(migrationPlanPersistencePort);
        this.cartographyPersistencePort = Objects.requireNonNull(cartographyPersistencePort);
        this.classificationPersistencePort = Objects.requireNonNull(classificationPersistencePort);
    }

    @Override
    public MigrationPlan handle(final String sessionId, final String controllerRef) {
        Optional<MigrationPlan> cached = migrationPlanPersistencePort.findBySessionId(sessionId);
        if (cached.isPresent()) {
            return cached.get();
        }

        int handlerCount = cartographyPersistencePort.findBySessionId(sessionId)
                .map(c -> c.handlers().size())
                .orElse(0);

        int ruleCount = classificationPersistencePort.findBySessionId(sessionId)
                .map(r -> r.rules().size() + r.uncertainRules().size())
                .orElse(0);

        int complexityScore = computeComplexityScore(handlerCount, ruleCount);

        final List<PlannedLot> lots = migrationPlannerPort.plan(
                controllerRef, handlerCount, ruleCount, complexityScore);
        final boolean compilable = !lots.isEmpty();
        MigrationPlan result = new MigrationPlan(controllerRef, lots, compilable);

        migrationPlanPersistencePort.save(sessionId, result);
        return result;
    }

    private int computeComplexityScore(final int handlerCount, final int ruleCount) {
        return handlerCount + ruleCount / 2;
    }
}
