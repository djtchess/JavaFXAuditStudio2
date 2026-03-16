package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.MigrationPlannerPort;
import ff.ss.javaFxAuditStudio.domain.migration.PlannedLot;
import ff.ss.javaFxAuditStudio.domain.migration.RegressionRisk;
import ff.ss.javaFxAuditStudio.domain.migration.RiskLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Planification contextuelle de lots de migration.
 * Le nombre de lots et les risques dependent de la complexite du controller.
 * Instancie par MigrationPlanConfiguration — pas d'annotation Spring.
 */
public final class RealMigrationPlannerAdapter implements MigrationPlannerPort {

    private static final int LOW_COMPLEXITY_THRESHOLD = 10;
    private static final int MEDIUM_COMPLEXITY_THRESHOLD = 30;
    private static final int LOW_RULE_THRESHOLD = 5;

    @Override
    public List<PlannedLot> plan(
            final String controllerRef,
            final int handlerCount,
            final int ruleCount,
            final int complexityScore) {
        int lotCount = determineLotCount(ruleCount, complexityScore);
        RiskLevel riskLevel = determineRiskLevel(complexityScore);
        List<PlannedLot> lots = new ArrayList<>();
        lots.add(buildDiagnosticLot(controllerRef, riskLevel, handlerCount, ruleCount));
        lots.add(buildFoundationLot(riskLevel));
        lots.add(buildMigrationLot(riskLevel, complexityScore));
        if (lotCount >= 4) {
            lots.add(buildAdapterLot(riskLevel));
        }
        if (lotCount >= 5) {
            lots.add(buildFinalizationLot(riskLevel));
        }
        return List.copyOf(lots);
    }

    private int determineLotCount(final int ruleCount, final int complexityScore) {
        if (complexityScore <= LOW_COMPLEXITY_THRESHOLD && ruleCount <= LOW_RULE_THRESHOLD) {
            return 3;
        }
        if (complexityScore <= MEDIUM_COMPLEXITY_THRESHOLD) {
            return 4;
        }
        return 5;
    }

    private RiskLevel determineRiskLevel(final int complexityScore) {
        if (complexityScore > 20) {
            return RiskLevel.HIGH;
        }
        if (complexityScore > 10) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private PlannedLot buildDiagnosticLot(
            final String controllerRef,
            final RiskLevel riskLevel,
            final int handlerCount,
            final int ruleCount) {
        String objective = "Diagnostic de " + shortRef(controllerRef)
                + " : " + handlerCount + " handlers, " + ruleCount + " regles";
        RegressionRisk risk = new RegressionRisk(
                "Risque de regression lors de l'analyse initiale",
                riskLevel,
                "Validation croisee cartographie/classification");
        return new PlannedLot(1, "Diagnostic et cible", objective,
                List.of("Cartographie des handlers",
                        "Identification des responsabilites melangees"),
                List.of(risk));
    }

    private PlannedLot buildFoundationLot(final RiskLevel riskLevel) {
        RegressionRisk risk = new RegressionRisk(
                "Risque de regression lors de la creation du socle",
                riskLevel,
                "Tests unitaires ViewModel et UseCase avant merge");
        return new PlannedLot(2, "ViewModel et premiers UseCase",
                "Creation du socle applicatif avec ViewModel et UseCases",
                List.of("ViewModel pour l'etat de presentation",
                        "UseCase pour les intentions utilisateur"),
                List.of(risk));
    }

    private PlannedLot buildMigrationLot(final RiskLevel riskLevel, final int complexityScore) {
        String objective = "Migration des handlers metier"
                + " (complexite=" + complexityScore + ")";
        RegressionRisk risk = new RegressionRisk(
                "Risque de regression sur la logique metier migree",
                riskLevel,
                "Tests de non-regression sur chaque handler migre");
        return new PlannedLot(3, "Handlers metier (BUSINESS, APPLICATION)", objective,
                List.of("Handlers lourds avec logique metier",
                        "Services applicatifs coordinateurs"),
                List.of(risk));
    }

    private PlannedLot buildAdapterLot(final RiskLevel riskLevel) {
        RegressionRisk risk = new RegressionRisk(
                "Risque de regression lors de l'introduction des Gateway",
                riskLevel,
                "Mocks des appels externes dans les tests");
        return new PlannedLot(4, "Gateway (appels REST, IO)",
                "Encapsulation des appels techniques dans des adaptateurs",
                List.of("Appels REST externes", "Acces fichiers et IO"),
                List.of(risk));
    }

    private PlannedLot buildFinalizationLot(final RiskLevel riskLevel) {
        RegressionRisk risk = new RegressionRisk(
                "Risque de regression lors de la finalisation",
                riskLevel,
                "Test d'integration bout-en-bout");
        return new PlannedLot(5, "Assemblers et Strategy",
                "Finalisation avec les assemblers de mapping et les strategies",
                List.of("Logique de mapping DTO/Domain",
                        "Strategies de selection de comportement"),
                List.of(risk));
    }

    private String shortRef(final String controllerRef) {
        if (controllerRef == null || controllerRef.isBlank()) {
            return "inconnu";
        }
        int lastSlash = Math.max(controllerRef.lastIndexOf('/'), controllerRef.lastIndexOf('\\'));
        String name = (lastSlash >= 0) ? controllerRef.substring(lastSlash + 1) : controllerRef;
        if (name.endsWith(".java")) {
            name = name.substring(0, name.length() - 5);
        }
        return name;
    }
}
