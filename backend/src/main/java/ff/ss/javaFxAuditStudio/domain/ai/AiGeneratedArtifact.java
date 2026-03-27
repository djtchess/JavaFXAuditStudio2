package ff.ss.javaFxAuditStudio.domain.ai;

import java.time.Instant;
import java.util.Objects;

/**
 * Version persistée d'un artefact généré ou raffiné par l'IA.
 */
public record AiGeneratedArtifact(
        String versionId,
        String sessionId,
        String artifactType,
        String className,
        String content,
        int versionNumber,
        String parentVersionId,
        String requestId,
        LlmProvider provider,
        TaskType originTask,
        Instant createdAt) {

    public AiGeneratedArtifact {
        Objects.requireNonNull(versionId, "versionId must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(artifactType, "artifactType must not be null");
        Objects.requireNonNull(className, "className must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(originTask, "originTask must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (artifactType.isBlank()) {
            throw new IllegalArgumentException("artifactType must not be blank");
        }
        if (className.isBlank()) {
            throw new IllegalArgumentException("className must not be blank");
        }
        if (versionNumber < 1) {
            throw new IllegalArgumentException("versionNumber must be >= 1");
        }
    }
}
