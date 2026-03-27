package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

@Schema(description = "Analyse differentielle entre deux lots de controllers")
public record ProjectDeltaResponse(
        @Schema(description = "Identifiant du projet")
        String projectId,
        @Schema(description = "Libelle du lot de reference")
        String baselineLabel,
        @Schema(description = "Libelle du lot courant")
        String currentLabel,
        @Schema(description = "Nombre de controllers nouveaux")
        int newControllers,
        @Schema(description = "Nombre de controllers supprimes")
        int removedControllers,
        @Schema(description = "Nombre de controllers modifies")
        int modifiedControllers,
        @Schema(description = "Nombre de controllers inchanges")
        int unchangedControllers,
        @Schema(description = "Details par controller")
        List<ControllerDeltaDto> controllerDeltas,
        @Schema(description = "Avertissements")
        List<String> warnings) {

    public ProjectDeltaResponse {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(baselineLabel, "baselineLabel must not be null");
        Objects.requireNonNull(currentLabel, "currentLabel must not be null");
        Objects.requireNonNull(controllerDeltas, "controllerDeltas must not be null");
        Objects.requireNonNull(warnings, "warnings must not be null");
        controllerDeltas = List.copyOf(controllerDeltas);
        warnings = List.copyOf(warnings);
    }

    @Schema(description = "Delta applique a un controller")
    public record ControllerDeltaDto(
            @Schema(description = "Reference du controller")
            String controllerRef,
            @Schema(description = "Statut du controller")
            String status,
            @Schema(description = "Regles ajoutees")
            List<String> addedRules,
            @Schema(description = "Regles retirees")
            List<String> removedRules,
            @Schema(description = "Transitions ajoutees")
            List<String> addedTransitions,
            @Schema(description = "Transitions retirees")
            List<String> removedTransitions,
            @Schema(description = "Notes")
            List<String> notes) {

        public ControllerDeltaDto {
            Objects.requireNonNull(controllerRef, "controllerRef must not be null");
            Objects.requireNonNull(status, "status must not be null");
            Objects.requireNonNull(addedRules, "addedRules must not be null");
            Objects.requireNonNull(removedRules, "removedRules must not be null");
            Objects.requireNonNull(addedTransitions, "addedTransitions must not be null");
            Objects.requireNonNull(removedTransitions, "removedTransitions must not be null");
            Objects.requireNonNull(notes, "notes must not be null");
            addedRules = List.copyOf(addedRules);
            removedRules = List.copyOf(removedRules);
            addedTransitions = List.copyOf(addedTransitions);
            removedTransitions = List.copyOf(removedTransitions);
            notes = List.copyOf(notes);
        }
    }
}
