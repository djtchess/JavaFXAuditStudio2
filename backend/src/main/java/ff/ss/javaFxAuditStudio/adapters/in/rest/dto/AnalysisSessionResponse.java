package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Reponse a la creation d'une session d'analyse")
public record AnalysisSessionResponse(
        @Schema(description = "Identifiant unique de la session (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,
        @Schema(description = "Statut initial de la session", example = "CREATED")
        String status,
        @Schema(description = "Nom de la session soumis par l'utilisateur")
        String sessionName,
        @Schema(description = "Reference du controller analyse (nom court du fichier Java)")
        String controllerRef) {

    public AnalysisSessionResponse {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(sessionName, "sessionName must not be null");
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
    }
}
