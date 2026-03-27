package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

@Schema(description = "Graphe des dependances inter-controllers d'un projet")
public record ProjectDependencyGraphResponse(
        @Schema(description = "Identifiant du projet")
        String projectId,
        @Schema(description = "Noeuds du graphe")
        List<ControllerNodeDto> controllers,
        @Schema(description = "Aretes de dependance")
        List<DependencyEdgeDto> dependencies,
        @Schema(description = "Ordre recommande de migration")
        List<String> recommendedOrder,
        @Schema(description = "Avertissements")
        List<String> warnings) {

    public ProjectDependencyGraphResponse {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(controllers, "controllers must not be null");
        Objects.requireNonNull(dependencies, "dependencies must not be null");
        Objects.requireNonNull(recommendedOrder, "recommendedOrder must not be null");
        Objects.requireNonNull(warnings, "warnings must not be null");
        controllers = List.copyOf(controllers);
        dependencies = List.copyOf(dependencies);
        recommendedOrder = List.copyOf(recommendedOrder);
        warnings = List.copyOf(warnings);
    }

    @Schema(description = "Noeud de controller")
    public record ControllerNodeDto(
            @Schema(description = "Reference du controller")
            String controllerRef,
            @Schema(description = "Nom simple du controller")
            String controllerName,
            @Schema(description = "Services injectes")
            List<String> injectedServices,
            @Schema(description = "Nombre de dependances sortantes")
            int outgoingDependencies,
            @Schema(description = "Nombre de dependances entrantes")
            int incomingDependencies) {

        public ControllerNodeDto {
            Objects.requireNonNull(controllerRef, "controllerRef must not be null");
            Objects.requireNonNull(controllerName, "controllerName must not be null");
            Objects.requireNonNull(injectedServices, "injectedServices must not be null");
            injectedServices = List.copyOf(injectedServices);
        }
    }

    @Schema(description = "Arete de dependance")
    public record DependencyEdgeDto(
            @Schema(description = "Controller source")
            String fromController,
            @Schema(description = "Controller cible")
            String toController,
            @Schema(description = "Type de dependance")
            String type,
            @Schema(description = "Preuve ou explication")
            String evidence) {

        public DependencyEdgeDto {
            Objects.requireNonNull(fromController, "fromController must not be null");
            Objects.requireNonNull(toController, "toController must not be null");
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(evidence, "evidence must not be null");
        }
    }
}
