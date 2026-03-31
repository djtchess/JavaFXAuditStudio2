package ff.ss.javaFxAuditStudio.adapters.out.observability;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionStatusHistoryPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;

/**
 * Health indicator qui resume l'etat du workflow d'analyse.
 */
public final class AnalysisWorkflowHealthIndicator implements HealthIndicator {

    private final AnalysisSessionPort analysisSessionPort;
    private final AnalysisSessionStatusHistoryPort statusHistoryPort;

    public AnalysisWorkflowHealthIndicator(
            final AnalysisSessionPort analysisSessionPort,
            final AnalysisSessionStatusHistoryPort statusHistoryPort) {
        this.analysisSessionPort = Objects.requireNonNull(analysisSessionPort, "analysisSessionPort must not be null");
        this.statusHistoryPort = (statusHistoryPort != null) ? statusHistoryPort : AnalysisSessionStatusHistoryPort.noop();
    }

    @Override
    public Health health() {
        try {
            List<AnalysisSession> sessions = analysisSessionPort.findAll();
            return Health.up()
                    .withDetail("totalSessions", sessions.size())
                    .withDetail("activeSessions", countActiveSessions(sessions))
                    .withDetail("completedSessions", countByStatus(sessions, AnalysisStatus.COMPLETED))
                    .withDetail("failedSessions", countByStatus(sessions, AnalysisStatus.FAILED))
                    .withDetail("lockedSessions", countByStatus(sessions, AnalysisStatus.LOCKED))
                    .withDetail("lastUpdateEpochMs", latestTimestamp(sessions))
                    .build();
        } catch (Exception ex) {
            return Health.down()
                    .withDetail("errorType", ex.getClass().getSimpleName())
                    .build();
        }
    }

    private static long countActiveSessions(final List<AnalysisSession> sessions) {
        return sessions.stream()
                .filter(session -> session.status().isPipelineActive())
                .count();
    }

    private static long countByStatus(
            final List<AnalysisSession> sessions,
            final AnalysisStatus status) {
        return sessions.stream()
                .filter(session -> session.status() == status)
                .count();
    }

    private long latestTimestamp(final List<AnalysisSession> sessions) {
        long latestSessionCreation = sessions.stream()
                .map(AnalysisSession::createdAt)
                .filter(Objects::nonNull)
                .mapToLong(Instant::toEpochMilli)
                .max()
                .orElse(0L);
        long latestTransition = statusHistoryPort.findLatestTransitionAt()
                .map(Instant::toEpochMilli)
                .orElse(0L);
        return Math.max(latestSessionCreation, latestTransition);
    }
}
