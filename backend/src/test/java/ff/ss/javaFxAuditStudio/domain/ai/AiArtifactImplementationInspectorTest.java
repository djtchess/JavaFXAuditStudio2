package ff.ss.javaFxAuditStudio.domain.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiArtifactImplementationInspectorTest {

    @Test
    void should_mark_artifact_as_incomplete_when_todo_placeholder_is_present() {
        String content = """
                package ff.example.policy;
                public class PatientPolicy {
                    // TODO: implementer
                    boolean isReady() {
                        return false;
                    }
                }
                """;

        assertThat(AiArtifactImplementationInspector.isIncomplete(content)).isTrue();
        assertThat(AiArtifactImplementationInspector.resolveStatus(content))
                .isEqualTo(AiArtifactImplementationStatus.INCOMPLETE);
        assertThat(AiArtifactImplementationInspector.resolveWarning(content))
                .contains("placeholder d'implementation");
    }

    @Test
    void should_mark_artifact_as_ready_when_no_placeholder_is_present() {
        String content = """
                package ff.example.usecase;
                public class PatientUseCase {
                    void execute() {
                    }
                }
                """;

        assertThat(AiArtifactImplementationInspector.isIncomplete(content)).isFalse();
        assertThat(AiArtifactImplementationInspector.resolveStatus(content))
                .isEqualTo(AiArtifactImplementationStatus.READY);
        assertThat(AiArtifactImplementationInspector.resolveWarning(content)).isNull();
    }
}
