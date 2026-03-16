package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.List;
import java.util.Objects;

public record MigrationPlanResponse(
        String controllerRef,
        boolean compilable,
        List<PlannedLotDto> lots) {

    public MigrationPlanResponse {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(lots, "lots must not be null");
        lots = List.copyOf(lots);
    }

    public record PlannedLotDto(
            int lotNumber,
            String title,
            String objective,
            List<String> extractionCandidates) {

        public PlannedLotDto {
            Objects.requireNonNull(title, "title must not be null");
            Objects.requireNonNull(objective, "objective must not be null");
            Objects.requireNonNull(extractionCandidates, "extractionCandidates must not be null");
            extractionCandidates = List.copyOf(extractionCandidates);
        }
    }
}
