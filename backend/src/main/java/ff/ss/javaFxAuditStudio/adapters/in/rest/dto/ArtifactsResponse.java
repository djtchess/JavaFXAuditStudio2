package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.List;
import java.util.Objects;

public record ArtifactsResponse(
        String controllerRef,
        List<CodeArtifactDto> artifacts,
        List<String> warnings) {

    public ArtifactsResponse {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(artifacts, "artifacts must not be null");
        Objects.requireNonNull(warnings, "warnings must not be null");
        artifacts = List.copyOf(artifacts);
        warnings = List.copyOf(warnings);
    }

    public record CodeArtifactDto(
            String artifactId,
            String type,
            int lotNumber,
            String className,
            boolean transitionalBridge) {

        public CodeArtifactDto {
            Objects.requireNonNull(artifactId, "artifactId must not be null");
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(className, "className must not be null");
        }
    }
}
