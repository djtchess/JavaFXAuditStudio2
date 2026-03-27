package ff.ss.javaFxAuditStudio.domain.ai;

import java.time.Instant;
import java.util.Objects;

import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;

/**
 * Exemple de code de reference fourni par l'equipe pour orienter les prompts IA.
 */
public record ProjectReferencePattern(
        String patternId,
        String artifactType,
        String referenceName,
        String content,
        Instant createdAt) {

    public ProjectReferencePattern {
        Objects.requireNonNull(patternId, "patternId must not be null");
        Objects.requireNonNull(artifactType, "artifactType must not be null");
        Objects.requireNonNull(referenceName, "referenceName must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        artifactType = artifactType.trim().toUpperCase();
        referenceName = referenceName.trim();
        content = content.replace("\r\n", "\n").replace("\r", "\n");
        ArtifactType.valueOf(artifactType);
        if (referenceName.isBlank()) {
            throw new IllegalArgumentException("referenceName must not be blank");
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }
}
