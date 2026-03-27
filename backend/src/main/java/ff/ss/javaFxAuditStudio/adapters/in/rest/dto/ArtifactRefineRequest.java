package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * Corps de requete pour le raffinement d'un artefact genere.
 */
@Schema(description = "Requete de raffinement d'un artefact Spring Boot genere")
public record ArtifactRefineRequest(
        @Schema(description = "Type d'artefact a raffiner", example = "USE_CASE")
        String artifactType,
        @Schema(description = "Instruction de raffinement fournie par l'utilisateur")
        String instruction,
        @Schema(description = "Code courant de l'artefact")
        String previousCode) {

    public ArtifactRefineRequest {
        Objects.requireNonNull(artifactType, "artifactType must not be null");
        Objects.requireNonNull(instruction, "instruction must not be null");
        Objects.requireNonNull(previousCode, "previousCode must not be null");
    }
}
