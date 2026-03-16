package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.MigrationPlannerPort;
import ff.ss.javaFxAuditStudio.domain.migration.PlannedLot;
import ff.ss.javaFxAuditStudio.domain.migration.RegressionRisk;
import ff.ss.javaFxAuditStudio.domain.migration.RiskLevel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Deprecated(forRemoval = true) // Suppression prevue JAS-301
@Profile("stub")
@Component
public class StubMigrationPlannerAdapter implements MigrationPlannerPort {

    private static final RegressionRisk GENERIC_LOW_RISK = new RegressionRisk(
            "Risque generique de regression a valider lors de l'integration",
            RiskLevel.LOW,
            "Couverture par tests unitaires avant merge"
    );

    @Override
    public List<PlannedLot> plan(
            final String controllerRef,
            final int handlerCount,
            final int ruleCount,
            final int complexityScore) {
        return List.of(
                new PlannedLot(1, "Diagnostic", "Stub diagnostic",
                        List.of("Stub"), List.of(GENERIC_LOW_RISK)),
                new PlannedLot(2, "Socle", "Stub socle",
                        List.of("Stub"), List.of(GENERIC_LOW_RISK)),
                new PlannedLot(3, "Migration", "Stub migration",
                        List.of("Stub"), List.of(GENERIC_LOW_RISK)),
                new PlannedLot(4, "Adaptateurs", "Stub adaptateurs",
                        List.of("Stub"), List.of(GENERIC_LOW_RISK)),
                new PlannedLot(5, "Finalisation", "Stub finalisation",
                        List.of("Stub"), List.of(GENERIC_LOW_RISK))
        );
    }
}
