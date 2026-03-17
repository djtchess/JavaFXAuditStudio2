package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.List;
import java.util.Objects;

public record ExportArtifactsResponse(
        String targetDirectory,
        List<String> exportedFiles,
        List<String> errors) {

    public ExportArtifactsResponse {
        Objects.requireNonNull(targetDirectory);
        Objects.requireNonNull(exportedFiles);
        Objects.requireNonNull(errors);
        exportedFiles = List.copyOf(exportedFiles);
        errors = List.copyOf(errors);
    }
}
