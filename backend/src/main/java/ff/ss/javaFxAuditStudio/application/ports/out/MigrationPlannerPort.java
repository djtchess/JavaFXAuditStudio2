package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.migration.PlannedLot;

import java.util.List;

public interface MigrationPlannerPort {

    /**
     * Planifie les lots de migration en fonction de la complexite du controller.
     *
     * @param controllerRef   reference du controller
     * @param handlerCount    nombre de handlers detectes
     * @param ruleCount       nombre de regles extraites
     * @param complexityScore score de complexite cyclomatique agrege
     * @return liste ordonnee de lots planifies, jamais null
     */
    List<PlannedLot> plan(String controllerRef, int handlerCount, int ruleCount, int complexityScore);

    /**
     * Compatibilite arriere : deleguee a la surcharge avec metriques par defaut.
     */
    default List<PlannedLot> plan(String controllerRef) {
        return plan(controllerRef, 0, 0, 0);
    }
}
