package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

@Schema(description = "Requete d'analyse des dependances inter-controllers")
public record ProjectDependenciesRequest(
        @Schema(description = "Identifiant du projet")
        String projectId,
        @Schema(description = "Liste des controllers a analyser")
        List<String> controllerRefs) {

    public ProjectDependenciesRequest {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(controllerRefs, "controllerRefs must not be null");
        controllerRefs = List.copyOf(controllerRefs);
    }
}
