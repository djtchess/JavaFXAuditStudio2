package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de restitution des versions d'artefacts IA persistés.
 */
@Schema(description = "Version persistée d'un artefact généré ou raffiné par l'IA")
public record AiGeneratedArtifactResponse(
        String artifactType,
        String className,
        String content,
        int versionNumber,
        String parentVersionId,
        String requestId,
        String provider,
        String originTask,
        Instant createdAt,
        String implementationStatus,
        String implementationWarning) {
}
