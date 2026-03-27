package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Exemple de code de reference pour orienter les prompts IA")
public record ProjectReferencePatternRequest(
        String artifactType,
        String referenceName,
        String content) {
}
