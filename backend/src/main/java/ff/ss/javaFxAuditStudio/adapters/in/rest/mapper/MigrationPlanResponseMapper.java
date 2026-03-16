package ff.ss.javaFxAuditStudio.adapters.in.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.MigrationPlanResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.MigrationPlanResponse.PlannedLotDto;
import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;
import ff.ss.javaFxAuditStudio.domain.migration.PlannedLot;

@Component
public class MigrationPlanResponseMapper {

    public MigrationPlanResponse toResponse(final MigrationPlan plan) {
        return new MigrationPlanResponse(
                plan.controllerRef(),
                plan.compilable(),
                mapLots(plan.lots()));
    }

    private List<PlannedLotDto> mapLots(final List<PlannedLot> lots) {
        return lots.stream()
                .map(lot -> new PlannedLotDto(
                        lot.lotNumber(),
                        lot.title(),
                        lot.objective(),
                        lot.extractionCandidates()))
                .toList();
    }
}
