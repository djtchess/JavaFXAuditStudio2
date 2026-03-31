package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionStatusHistoryPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatusTransition;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class JpaAnalysisSessionStatusHistoryAdapter implements AnalysisSessionStatusHistoryPort {

    private final AnalysisSessionStatusHistoryRepository repository;

    public JpaAnalysisSessionStatusHistoryAdapter(final AnalysisSessionStatusHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public AnalysisStatusTransition save(final AnalysisStatusTransition transition) {
        AnalysisSessionStatusHistoryEntity saved;

        saved = repository.save(toEntity(transition));
        return toDomain(saved);
    }

    @Override
    public List<AnalysisStatusTransition> findBySessionId(final String sessionId) {
        return repository.findBySessionIdOrderByOccurredAtAsc(sessionId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Instant> findLatestTransitionAt() {
        return repository.findTopByOrderByOccurredAtDesc()
                .map(AnalysisSessionStatusHistoryEntity::getOccurredAt);
    }

    private AnalysisSessionStatusHistoryEntity toEntity(final AnalysisStatusTransition transition) {
        return new AnalysisSessionStatusHistoryEntity(
                transition.sessionId(),
                transition.status().name(),
                transition.occurredAt());
    }

    private AnalysisStatusTransition toDomain(final AnalysisSessionStatusHistoryEntity entity) {
        return new AnalysisStatusTransition(
                entity.getSessionId(),
                AnalysisStatus.valueOf(entity.getStatus()),
                entity.getOccurredAt());
    }
}
