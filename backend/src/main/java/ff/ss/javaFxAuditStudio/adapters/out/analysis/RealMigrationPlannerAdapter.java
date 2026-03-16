package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.MigrationPlannerPort;
import ff.ss.javaFxAuditStudio.domain.migration.PlannedLot;
import ff.ss.javaFxAuditStudio.domain.migration.RegressionRisk;
import ff.ss.javaFxAuditStudio.domain.migration.RiskLevel;

import java.util.List;

public final class RealMigrationPlannerAdapter implements MigrationPlannerPort {

    private static final String MITIGATION = "Couverture par tests unitaires";

    @Override
    public List<PlannedLot> plan(final String controllerRef) {
        return List.of(
                buildLot1(controllerRef),
                buildLot2(controllerRef),
                buildLot3(controllerRef),
                buildLot4(controllerRef),
                buildLot5(controllerRef)
        );
    }

    private RiskLevel resolveLevel(final String controllerRef, final RiskLevel defaultLevel) {
        if (controllerRef != null && controllerRef.contains("Complex")) {
            return RiskLevel.HIGH;
        }
        return defaultLevel;
    }

    private PlannedLot buildLot1(final String controllerRef) {
        RiskLevel level = resolveLevel(controllerRef, RiskLevel.LOW);
        RegressionRisk risk = new RegressionRisk(
                "Risque de regression lors du diagnostic initial",
                level,
                MITIGATION
        );
        return new PlannedLot(
                1,
                "Diagnostic et cible",
                "diagnostic",
                List.of(
                        "Extraction des appels directs à la couche service",
                        "Identification des responsabilités mélangées"
                ),
                List.of(risk)
        );
    }

    private PlannedLot buildLot2(final String controllerRef) {
        RiskLevel level = resolveLevel(controllerRef, RiskLevel.LOW);
        RegressionRisk risk = new RegressionRisk(
                "Risque de regression lors de la création du socle ViewModel et UseCase",
                level,
                MITIGATION
        );
        return new PlannedLot(
                2,
                "ViewModel et premiers UseCase",
                "socle",
                List.of(
                        "ViewModel pour l'état de présentation",
                        "UseCase pour les intentions utilisateur"
                ),
                List.of(risk)
        );
    }

    private PlannedLot buildLot3(final String controllerRef) {
        RiskLevel level = resolveLevel(controllerRef, RiskLevel.MEDIUM);
        RegressionRisk risk = new RegressionRisk(
                "Risque de regression lors de la migration des handlers métier",
                level,
                MITIGATION
        );
        return new PlannedLot(
                3,
                "Handlers métier (BUSINESS, APPLICATION)",
                "migration effective",
                List.of(
                        "Handlers lourds avec logique métier",
                        "Services applicatifs coordinateurs"
                ),
                List.of(risk)
        );
    }

    private PlannedLot buildLot4(final String controllerRef) {
        RiskLevel level = resolveLevel(controllerRef, RiskLevel.MEDIUM);
        RegressionRisk risk = new RegressionRisk(
                "Risque de regression lors de l'introduction des Gateway",
                level,
                MITIGATION
        );
        return new PlannedLot(
                4,
                "Gateway (appels REST, IO)",
                "adaptateurs",
                List.of(
                        "Appels REST externes",
                        "Accès fichiers et IO"
                ),
                List.of(risk)
        );
    }

    private PlannedLot buildLot5(final String controllerRef) {
        RiskLevel level = resolveLevel(controllerRef, RiskLevel.LOW);
        RegressionRisk risk = new RegressionRisk(
                "Risque de regression lors de la finalisation des Assemblers et Strategy",
                level,
                MITIGATION
        );
        return new PlannedLot(
                5,
                "Assemblers et Strategy",
                "finalisation",
                List.of(
                        "Logique de mapping DTO/Domain",
                        "Stratégies de sélection de comportement"
                ),
                List.of(risk)
        );
    }
}
