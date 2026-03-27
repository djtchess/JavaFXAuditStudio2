package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Liste des patterns projet disponibles pour les prompts IA")
public record ProjectReferencePatternCollectionResponse(
        List<ProjectReferencePatternResponse> patterns) {
}
