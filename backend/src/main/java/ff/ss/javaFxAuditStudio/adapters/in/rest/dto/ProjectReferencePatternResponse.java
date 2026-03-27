package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pattern projet persiste pour guider les generations IA")
public record ProjectReferencePatternResponse(
        String patternId,
        String artifactType,
        String referenceName,
        String content,
        Instant createdAt) {
}
