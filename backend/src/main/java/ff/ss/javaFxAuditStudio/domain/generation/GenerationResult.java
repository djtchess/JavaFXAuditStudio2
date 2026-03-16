package ff.ss.javaFxAuditStudio.domain.generation;

import java.util.List;
import java.util.Objects;

public record GenerationResult(
        String controllerRef,
        List<CodeArtifact> artifacts,
        List<String> warnings
) {
    public GenerationResult {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        artifacts = (artifacts != null) ? List.copyOf(artifacts) : List.of();
        warnings = (warnings != null) ? List.copyOf(warnings) : List.of();
    }

    public List<CodeArtifact> artifactsByLot(final int lotNumber) {
        return artifacts.stream()
                .filter(a -> a.lotNumber() == lotNumber)
                .toList();
    }

    public List<CodeArtifact> bridges() {
        return artifacts.stream()
                .filter(CodeArtifact::transitionalBridge)
                .toList();
    }
}
