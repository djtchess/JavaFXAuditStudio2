package ff.ss.javaFxAuditStudio.domain.ingestion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class IngestionResultTest {

    @Test
    void hasErrors_returnsFalse_whenNoErrors() {
        SourceInput input;
        IngestionResult result;

        input = new SourceInput("com/example/MyController.java", SourceInputType.JAVA_CONTROLLER, "class MyController {}");
        result = new IngestionResult(List.of(input), List.of());

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void hasErrors_returnsTrue_whenErrorsPresent() {
        IngestionError error;
        IngestionResult result;

        error = new IngestionError(IngestionErrorCode.FILE_NOT_FOUND, "com/example/Missing.java");
        result = new IngestionResult(List.of(), List.of(error));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void constructor_throwsNullPointerException_whenControllerRefNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SourceInput(null, SourceInputType.JAVA_CONTROLLER, "content"))
                .withMessageContaining("ref");
    }
}
