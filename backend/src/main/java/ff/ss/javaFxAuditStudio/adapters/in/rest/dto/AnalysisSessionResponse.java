package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Objects;

@Schema(description = "Reponse a la creation d'une session d'analyse")
public record AnalysisSessionResponse(
        @Schema(description = "Identifiant unique de la session (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,
        @Schema(description = "Statut initial de la session", example = "CREATED")
        String status,
        @Schema(description = "Nom de la session soumis par l'utilisateur")
        String sessionName,
        @Schema(description = "Reference du controller analyse (chemin ou identifiant JavaFX)")
        String controllerRef,
        @Schema(description = "Reference technique complementaire (FXML, snippet ou source secondaire)")
        String sourceSnippetRef,
        @Schema(description = "Horodatage ISO-8601 de creation de la session")
        Instant createdAt) {

    public AnalysisSessionResponse {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(sessionName, "sessionName must not be null");
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        sourceSnippetRef = sourceSnippetRef == null ? "" : sourceSnippetRef;
    }
}
