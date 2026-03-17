package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.Objects;

public record ExportArtifactsRequest(String targetDirectory) {
    public ExportArtifactsRequest {
        Objects.requireNonNull(targetDirectory, "targetDirectory must not be null");
    }
}
