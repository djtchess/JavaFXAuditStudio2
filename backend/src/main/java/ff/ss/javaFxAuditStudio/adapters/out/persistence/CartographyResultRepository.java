package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartographyResultRepository extends JpaRepository<CartographyResultEntity, Long> {

    Optional<CartographyResultEntity> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
