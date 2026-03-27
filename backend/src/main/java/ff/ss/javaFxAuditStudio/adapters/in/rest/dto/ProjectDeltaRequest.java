package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

@Schema(description = "Requete d'analyse differentielle entre deux lots de controllers")
public record ProjectDeltaRequest(
        @Schema(description = "Identifiant du projet")
        String projectId,
        @Schema(description = "Controllers de reference")
        List<String> baselineControllerRefs,
        @Schema(description = "Controllers courants")
        List<String> currentControllerRefs) {

    public ProjectDeltaRequest {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(baselineControllerRefs, "baselineControllerRefs must not be null");
        Objects.requireNonNull(currentControllerRefs, "currentControllerRefs must not be null");
        baselineControllerRefs = List.copyOf(baselineControllerRefs);
        currentControllerRefs = List.copyOf(currentControllerRefs);
    }
}
