package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAnalysisSessionServiceTest {

    @Mock
    private AnalysisSessionPort analysisSessionPort;

    @InjectMocks
    private GetAnalysisSessionService service;

    @Test
    void handle_returnsSessionWhenItExists() {
        AnalysisSession session = new AnalysisSession(
                "session-1",
                "Audit MainController",
                "src/main/java/com/app/MainController.java",
                "src/main/resources/view/Main.fxml",
                AnalysisStatus.CREATED,
                Instant.parse("2026-03-30T10:15:00Z"));

        when(analysisSessionPort.findById("session-1")).thenReturn(Optional.of(session));

        Optional<AnalysisSession> result = service.handle("session-1");

        assertThat(result).contains(session);
    }

    @Test
    void handle_returnsEmptyWhenSessionDoesNotExist() {
        when(analysisSessionPort.findById("missing-session")).thenReturn(Optional.empty());

        Optional<AnalysisSession> result = service.handle("missing-session");

        assertThat(result).isEmpty();
    }
}
