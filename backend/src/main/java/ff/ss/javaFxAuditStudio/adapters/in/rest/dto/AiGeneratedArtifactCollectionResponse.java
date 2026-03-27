package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de liste des artefacts IA persistés pour une session.
 */
@Schema(description = "Collection d'artefacts IA persistés pour une session")
public record AiGeneratedArtifactCollectionResponse(
        String sessionId,
        List<AiGeneratedArtifactResponse> artifacts) {
}
