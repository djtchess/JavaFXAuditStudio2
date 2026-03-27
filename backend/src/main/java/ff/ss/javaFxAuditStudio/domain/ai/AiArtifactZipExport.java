package ff.ss.javaFxAuditStudio.domain.ai;

import java.util.Arrays;
import java.util.Objects;

/**
 * Archive ZIP des artefacts IA persistés.
 */
public record AiArtifactZipExport(
        String fileName,
        byte[] content,
        int artifactCount) {

    public AiArtifactZipExport {
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (artifactCount < 0) {
            throw new IllegalArgumentException("artifactCount must be >= 0");
        }
        content = Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
