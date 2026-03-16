package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.migration.PlannedLot;

import java.util.List;

public interface MigrationPlannerPort {

    List<PlannedLot> plan(String controllerRef);
}
