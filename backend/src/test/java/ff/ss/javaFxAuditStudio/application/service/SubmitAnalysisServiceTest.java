package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionStatusHistoryPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatusTransition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitAnalysisServiceTest {

    @Mock
    private AnalysisSessionPort analysisSessionPort;

    @Mock
    private AnalysisSessionStatusHistoryPort statusHistoryPort;

    @InjectMocks
    private SubmitAnalysisService service;

    @Test
    void handle_createsCreatedSessionAndPersistsInitialTransition() {
        when(analysisSessionPort.save(any(AnalysisSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statusHistoryPort.save(any(AnalysisStatusTransition.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnalysisSession result = service.handle(
                List.of(
                        "src/main/resources/view/Main.fxml",
                        "src/main/java/com/app/MainController.java"),
                "Audit MainController");

        ArgumentCaptor<AnalysisSession> sessionCaptor = ArgumentCaptor.forClass(AnalysisSession.class);
        ArgumentCaptor<AnalysisStatusTransition> transitionCaptor = ArgumentCaptor.forClass(AnalysisStatusTransition.class);

        verify(analysisSessionPort).save(sessionCaptor.capture());
        verify(statusHistoryPort).save(transitionCaptor.capture());

        assertThat(result.status()).isEqualTo(AnalysisStatus.CREATED);
        assertThat(result.sessionName()).isEqualTo("Audit MainController");
        assertThat(result.controllerName()).isEqualTo("src/main/java/com/app/MainController.java");
        assertThat(result.sourceSnippetRef()).isEqualTo("src/main/resources/view/Main.fxml");
        assertThat(result.sessionId()).isNotBlank();
        assertThat(result.createdAt()).isNotNull();
        assertThat(sessionCaptor.getValue().status()).isEqualTo(AnalysisStatus.CREATED);
        assertThat(transitionCaptor.getValue().status()).isEqualTo(AnalysisStatus.CREATED);
        assertThat(transitionCaptor.getValue().sessionId()).isEqualTo(result.sessionId());
    }

    @Test
    void handle_throwsWhenSourceFilePathsAreEmpty() {
        assertThatThrownBy(() -> service.handle(List.of(" ", "   "), "Audit MainController"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceFilePaths");
    }
}
