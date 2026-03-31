package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisSessionStatusHistoryRepository extends JpaRepository<AnalysisSessionStatusHistoryEntity, Long> {

    List<AnalysisSessionStatusHistoryEntity> findBySessionIdOrderByOccurredAtAsc(String sessionId);

    Optional<AnalysisSessionStatusHistoryEntity> findTopByOrderByOccurredAtDesc();
}
