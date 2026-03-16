package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.MigrationPlannerPort;
import ff.ss.javaFxAuditStudio.domain.migration.PlannedLot;
import ff.ss.javaFxAuditStudio.domain.migration.RegressionRisk;
import ff.ss.javaFxAuditStudio.domain.migration.RiskLevel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Profile("stub")
@Component
public class StubMigrationPlannerAdapter implements MigrationPlannerPort {

    private static final RegressionRisk GENERIC_LOW_RISK = new RegressionRisk(
            "Risque générique de régression à valider lors de l'intégration",
            RiskLevel.LOW,
            "Couverture par tests unitaires avant merge"
    );

    @Override
    public List<PlannedLot> plan(final String controllerRef) {
        return List.of(
                buildLot1(),
                buildLot2(),
                buildLot3(),
                buildLot4(),
                buildLot5()
        );
    }

    private PlannedLot buildLot1() {
        return new PlannedLot(
                1,
                "Diagnostic",
                "Identifier les responsabilités mélangées et les dépendances directes dans le controller",
                List.of("Extraction des appels directs à la couche service"),
                List.of(GENERIC_LOW_RISK)
        );
    }

    private PlannedLot buildLot2() {
        return new PlannedLot(
                2,
                "Socle applicatif",
                "Créer les ports entrants et sortants, le service applicatif et la configuration Spring",
                List.of("Interfaces UseCase", "Interfaces Port sortant"),
                List.of(GENERIC_LOW_RISK)
        );
    }

    private PlannedLot buildLot3() {
        return new PlannedLot(
                3,
                "Flux majeurs",
                "Migrer les flux métier principaux vers les use cases applicatifs",
                List.of("Logique métier centrale du controller"),
                List.of(GENERIC_LOW_RISK)
        );
    }

    private PlannedLot buildLot4() {
        return new PlannedLot(
                4,
                "Adaptateurs Spring",
                "Remplacer les dépendances Spring directes par des adapters dédiés",
                List.of("Appels @Autowired directs", "Accès aux beans Spring dans le controller"),
                List.of(GENERIC_LOW_RISK)
        );
    }

    private PlannedLot buildLot5() {
        return new PlannedLot(
                5,
                "Assemblers et stratégies",
                "Introduire les assemblers de mapping et les stratégies pour finaliser l'hexagone",
                List.of("Logique de mapping DTO/Domain", "Stratégies de sélection de comportement"),
                List.of(GENERIC_LOW_RISK)
        );
    }
}
