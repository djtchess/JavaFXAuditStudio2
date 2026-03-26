package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Requete de soumission d'une session d'analyse JavaFX")
public record SubmitAnalysisRequest(
        @Schema(description = "Chemins absolus des fichiers .java source a analyser", example = "[\"C:/project/src/PatientController.java\"]")
        List<String> sourceFilePaths,
        @Schema(description = "Nom descriptif de la session", example = "Analyse PatientController v1")
        String sessionName) {

    public SubmitAnalysisRequest {
        Objects.requireNonNull(sourceFilePaths, "sourceFilePaths must not be null");
        Objects.requireNonNull(sessionName, "sessionName must not be null");
        if (sourceFilePaths.isEmpty()) {
            throw new IllegalArgumentException("sourceFilePaths must not be empty");
        }
        sourceFilePaths = List.copyOf(sourceFilePaths);
    }
}
