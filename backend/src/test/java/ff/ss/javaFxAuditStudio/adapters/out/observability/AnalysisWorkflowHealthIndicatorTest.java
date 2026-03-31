package ff.ss.javaFxAuditStudio.adapters.out.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionStatusHistoryPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;

class AnalysisWorkflowHealthIndicatorTest {

    @Test
    void health_reportsSessionCounters() {
        AnalysisSessionPort sessionPort;
        AnalysisSessionStatusHistoryPort historyPort;
        AnalysisWorkflowHealthIndicator indicator;
        Health health;

        sessionPort = Mockito.mock(AnalysisSessionPort.class);
        historyPort = Mockito.mock(AnalysisSessionStatusHistoryPort.class);
        when(sessionPort.findAll()).thenReturn(List.of(
                session("s1", AnalysisStatus.CREATED),
                session("s2", AnalysisStatus.IN_PROGRESS),
                session("s3", AnalysisStatus.INGESTING),
                session("s4", AnalysisStatus.COMPLETED),
                session("s5", AnalysisStatus.FAILED)));
        when(historyPort.findLatestTransitionAt()).thenReturn(Optional.empty());
        indicator = new AnalysisWorkflowHealthIndicator(sessionPort, historyPort);

        health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("totalSessions", 5);
        assertThat(health.getDetails()).containsEntry("activeSessions", 2L);
        assertThat(health.getDetails()).containsEntry("completedSessions", 1L);
        assertThat(health.getDetails()).containsEntry("failedSessions", 1L);
    }

    @Test
    void health_isDown_whenRepositoryFails() {
        AnalysisSessionPort sessionPort;
        AnalysisSessionStatusHistoryPort historyPort;
        AnalysisWorkflowHealthIndicator indicator;

        sessionPort = Mockito.mock(AnalysisSessionPort.class);
        historyPort = Mockito.mock(AnalysisSessionStatusHistoryPort.class);
        when(sessionPort.findAll()).thenThrow(new IllegalStateException("db unavailable"));
        when(historyPort.findLatestTransitionAt()).thenReturn(Optional.empty());
        indicator = new AnalysisWorkflowHealthIndicator(sessionPort, historyPort);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }

    private static AnalysisSession session(final String sessionId, final AnalysisStatus status) {
        return new AnalysisSession(sessionId, "Controller.java", null, status, Instant.now());
    }
}
