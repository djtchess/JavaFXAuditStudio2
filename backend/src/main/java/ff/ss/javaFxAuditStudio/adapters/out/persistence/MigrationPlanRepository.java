package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MigrationPlanRepository extends JpaRepository<MigrationPlanEntity, Long> {

    Optional<MigrationPlanEntity> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
