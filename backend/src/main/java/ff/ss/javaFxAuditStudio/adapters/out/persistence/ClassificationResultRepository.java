package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassificationResultRepository extends JpaRepository<ClassificationResultEntity, Long> {

    Optional<ClassificationResultEntity> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
