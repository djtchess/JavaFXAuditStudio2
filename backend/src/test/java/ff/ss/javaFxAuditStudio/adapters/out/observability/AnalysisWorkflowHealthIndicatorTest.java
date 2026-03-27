package ff.ss.javaFxAuditStudio.adapters.out.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;

class AnalysisWorkflowHealthIndicatorTest {

    @Test
    void health_reportsSessionCounters() {
        AnalysisSessionPort sessionPort;
        AnalysisWorkflowHealthIndicator indicator;
        Health health;

        sessionPort = Mockito.mock(AnalysisSessionPort.class);
        when(sessionPort.findAll()).thenReturn(List.of(
                session("s1", AnalysisStatus.CREATED),
                session("s2", AnalysisStatus.INGESTING),
                session("s3", AnalysisStatus.COMPLETED),
                session("s4", AnalysisStatus.FAILED)));
        indicator = new AnalysisWorkflowHealthIndicator(sessionPort);

        health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("totalSessions", 4);
        assertThat(health.getDetails()).containsEntry("activeSessions", 1L);
        assertThat(health.getDetails()).containsEntry("completedSessions", 1L);
        assertThat(health.getDetails()).containsEntry("failedSessions", 1L);
    }

    @Test
    void health_isDown_whenRepositoryFails() {
        AnalysisSessionPort sessionPort;
        AnalysisWorkflowHealthIndicator indicator;

        sessionPort = Mockito.mock(AnalysisSessionPort.class);
        when(sessionPort.findAll()).thenThrow(new IllegalStateException("db unavailable"));
        indicator = new AnalysisWorkflowHealthIndicator(sessionPort);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }

    private static AnalysisSession session(final String sessionId, final AnalysisStatus status) {
        return new AnalysisSession(sessionId, "Controller.java", null, status, Instant.now());
    }
}
