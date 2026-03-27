package ff.ss.javaFxAuditStudio.domain.ai;

import java.util.Objects;

import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;

/**
 * Commande de raffinement multi-tour d'un artefact IA.
 */
public record AiArtifactRefinementCommand(
        String artifactType,
        String instruction,
        String previousCode) {

    public AiArtifactRefinementCommand {
        Objects.requireNonNull(artifactType, "artifactType must not be null");
        Objects.requireNonNull(instruction, "instruction must not be null");
        artifactType = artifactType.trim().toUpperCase();
        instruction = instruction.trim();
        previousCode = (previousCode != null) ? previousCode : "";
        if (artifactType.isBlank()) {
            throw new IllegalArgumentException("artifactType must not be blank");
        }
        if (instruction.isBlank()) {
            throw new IllegalArgumentException("instruction must not be blank");
        }
        ArtifactType.valueOf(artifactType);
    }
}
