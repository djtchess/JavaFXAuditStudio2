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

    /**
     * DTO d'un artefact genere, enrichi des avertissements de validation (JAS-009).
     */
    public record CodeArtifactDto(
            String artifactId,
            String type,
            int lotNumber,
            String className,
            String content,
            boolean transitionalBridge,
            List<String> generationWarnings,
            String generationStatus) {

        public CodeArtifactDto {
            Objects.requireNonNull(artifactId, "artifactId must not be null");
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(className, "className must not be null");
            Objects.requireNonNull(content, "content must not be null");
            generationWarnings = (generationWarnings != null) ? List.copyOf(generationWarnings) : List.of();
            generationStatus = (generationStatus != null) ? generationStatus : "OK";
        }
    }
}
