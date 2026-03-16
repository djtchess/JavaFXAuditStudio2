package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;

import java.util.Optional;

/**
 * Port sortant pour la persistence des plans de migration.
 */
public interface MigrationPlanPersistencePort {

    MigrationPlan save(String sessionId, MigrationPlan plan);

    Optional<MigrationPlan> findBySessionId(String sessionId);
}
