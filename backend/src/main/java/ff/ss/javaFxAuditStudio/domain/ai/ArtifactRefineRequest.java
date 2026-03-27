package ff.ss.javaFxAuditStudio.domain.ai;

import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;

import java.util.Objects;

/**
 * Requete metier pour le raffinement d'un artefact Spring Boot genere.
 *
 * @param artifactType  type d'artefact a raffiner
 * @param instruction   consigne de raffinement fournie par l'utilisateur
 * @param previousCode   code actuel de l'artefact
 */
public record ArtifactRefineRequest(
        ArtifactType artifactType,
        String instruction,
        String previousCode) {

    public ArtifactRefineRequest {
        Objects.requireNonNull(artifactType, "artifactType must not be null");
        Objects.requireNonNull(instruction, "instruction must not be null");
        Objects.requireNonNull(previousCode, "previousCode must not be null");
    }
}
