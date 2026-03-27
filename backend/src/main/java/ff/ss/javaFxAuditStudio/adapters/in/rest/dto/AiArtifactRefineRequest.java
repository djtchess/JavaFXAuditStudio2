package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Requête REST de raffinement multi-tour d'un artefact IA.
 */
@Schema(description = "Commande de raffinement d'un artefact IA")
public record AiArtifactRefineRequest(
        String artifactType,
        String instruction,
        String previousCode) {
}
