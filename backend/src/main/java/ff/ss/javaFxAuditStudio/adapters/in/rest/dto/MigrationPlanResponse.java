package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Plan de migration par lots")
public record MigrationPlanResponse(
        @Schema(description = "Reference du controller source de l'analyse")
        String controllerRef,
        @Schema(description = "Indique si le plan genere est compilable")
        boolean compilable,
        @Schema(description = "Liste des lots planifies dans la migration")
        List<PlannedLotDto> lots) {

    public MigrationPlanResponse {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(lots, "lots must not be null");
        lots = List.copyOf(lots);
    }

    @Schema(description = "Lot planifie dans le plan de migration")
    public record PlannedLotDto(
            @Schema(description = "Numero de lot (1 a 5)")
            int lotNumber,
            @Schema(description = "Titre court du lot")
            String title,
            @Schema(description = "Objectif principal du lot")
            String objective,
            @Schema(description = "Liste des candidats d'extraction affectes a ce lot")
            List<String> extractionCandidates) {

        public PlannedLotDto {
            Objects.requireNonNull(title, "title must not be null");
            Objects.requireNonNull(objective, "objective must not be null");
            Objects.requireNonNull(extractionCandidates, "extractionCandidates must not be null");
            extractionCandidates = List.copyOf(extractionCandidates);
        }
    }
}
