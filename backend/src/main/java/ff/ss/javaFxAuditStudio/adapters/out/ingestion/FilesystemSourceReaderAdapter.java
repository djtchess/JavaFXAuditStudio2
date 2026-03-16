package ff.ss.javaFxAuditStudio.adapters.out.ingestion;

import ff.ss.javaFxAuditStudio.application.ports.out.SourceReaderPort;
import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInput;
import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInputType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
public class FilesystemSourceReaderAdapter implements SourceReaderPort {

    @Override
    public Optional<SourceInput> read(final String ref) {
        Path path = Path.of(ref);
        Optional<SourceInputType> typeOpt = resolveType(ref);
        if (typeOpt.isEmpty()) {
            return Optional.empty();
        }
        return readContent(path, ref, typeOpt.get());
    }

    private Optional<SourceInputType> resolveType(final String ref) {
        if (ref.endsWith(".java")) {
            return Optional.of(SourceInputType.JAVA_CONTROLLER);
        }
        if (ref.endsWith(".fxml")) {
            return Optional.of(SourceInputType.FXML);
        }
        if (ref.endsWith(".properties")) {
            return Optional.of(SourceInputType.SPRING_PROPERTIES);
        }
        return Optional.empty();
    }

    private Optional<SourceInput> readContent(final Path path, final String ref, final SourceInputType type) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return Optional.of(new SourceInput(ref, type, content));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
