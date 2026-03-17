package ff.ss.javaFxAuditStudio.domain.generation;

import java.util.List;
import java.util.Objects;

public record ExportResult(
        String targetDirectory,
        List<String> exportedFiles,
        List<String> errors) {

    public ExportResult {
        Objects.requireNonNull(targetDirectory);
        Objects.requireNonNull(exportedFiles);
        Objects.requireNonNull(errors);
        exportedFiles = List.copyOf(exportedFiles);
        errors = List.copyOf(errors);
    }
}
