package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Requete d'export des artefacts generes vers un repertoire")
public record ExportArtifactsRequest(
        @Schema(description = "Chemin absolu du repertoire de destination", example = "C:/project/generated")
        String targetDirectory) {
    public ExportArtifactsRequest {
        Objects.requireNonNull(targetDirectory, "targetDirectory must not be null");
    }
}
