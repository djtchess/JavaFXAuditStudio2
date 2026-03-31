package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatusTransition;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AnalysisSessionStatusHistoryPort {

    AnalysisStatusTransition save(AnalysisStatusTransition transition);

    List<AnalysisStatusTransition> findBySessionId(String sessionId);

    Optional<Instant> findLatestTransitionAt();

    static AnalysisSessionStatusHistoryPort noop() {
        return new AnalysisSessionStatusHistoryPort() {
            @Override
            public AnalysisStatusTransition save(final AnalysisStatusTransition transition) {
                return transition;
            }

            @Override
            public List<AnalysisStatusTransition> findBySessionId(final String sessionId) {
                return List.of();
            }

            @Override
            public Optional<Instant> findLatestTransitionAt() {
                return Optional.empty();
            }
        };
    }
}
