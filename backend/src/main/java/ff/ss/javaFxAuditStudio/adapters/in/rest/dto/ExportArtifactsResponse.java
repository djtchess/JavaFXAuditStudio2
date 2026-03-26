package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Resultat de l'export des artefacts")
public record ExportArtifactsResponse(
        @Schema(description = "Chemin absolu du repertoire de destination utilise")
        String targetDirectory,
        @Schema(description = "Liste des fichiers effectivement ecrits lors de l'export")
        List<String> exportedFiles,
        @Schema(description = "Liste des erreurs rencontrees lors de l'export")
        List<String> errors) {

    public ExportArtifactsResponse {
        Objects.requireNonNull(targetDirectory);
        Objects.requireNonNull(exportedFiles);
        Objects.requireNonNull(errors);
        exportedFiles = List.copyOf(exportedFiles);
        errors = List.copyOf(errors);
    }
}
