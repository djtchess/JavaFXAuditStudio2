package ff.ss.javaFxAuditStudio.domain.generation;

import java.util.Objects;

public record CodeArtifact(
        String artifactId,
        ArtifactType type,
        int lotNumber,
        String className,
        String content,
        boolean transitionalBridge
) {
    public CodeArtifact {
        Objects.requireNonNull(artifactId, "artifactId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(className, "className must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (lotNumber < 1 || lotNumber > 5) {
            throw new IllegalArgumentException("lotNumber must be between 1 and 5, got: " + lotNumber);
        }
    }
}
