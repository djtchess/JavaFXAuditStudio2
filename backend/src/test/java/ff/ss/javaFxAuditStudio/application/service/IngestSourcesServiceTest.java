package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.out.SourceReaderPort;
import ff.ss.javaFxAuditStudio.domain.ingestion.IngestionResult;
import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInput;
import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInputType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class IngestSourcesServiceTest {

    @Test
    void handle_returnsErrorResult_whenRefNotFound() {
        SourceReaderPort port;
        IngestSourcesService service;
        IngestionResult result;

        port = ref -> Optional.empty();
        service = new IngestSourcesService(port);

        result = service.handle(List.of("com/example/Missing.java"));

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.inputs()).isEmpty();
    }

    @Test
    void handle_returnsInputResult_whenRefFound() {
        SourceInput sourceInput;
        SourceReaderPort port;
        IngestSourcesService service;
        IngestionResult result;

        sourceInput = new SourceInput(
                "com/example/MyController.java",
                SourceInputType.JAVA_CONTROLLER,
                "class MyController {}");
        port = ref -> Optional.of(sourceInput);
        service = new IngestSourcesService(port);

        result = service.handle(List.of("com/example/MyController.java"));

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.inputs()).hasSize(1);
        assertThat(result.inputs().get(0).ref()).isEqualTo("com/example/MyController.java");
    }

    @Test
    void handle_returnsEmptyResult_whenNoRefs() {
        SourceReaderPort port;
        IngestSourcesService service;
        IngestionResult result;

        port = ref -> Optional.empty();
        service = new IngestSourcesService(port);

        result = service.handle(List.of());

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.inputs()).isEmpty();
        assertThat(result.errors()).isEmpty();
    }
}
