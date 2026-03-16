package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;

/**
 * Port entrant pour la production d'une restitution finale.
 * Declenche l'analyse, la consolidation et la mise en forme
 * d'un rapport exploitable pour le controller donne.
 */
public interface ProduceRestitutionUseCase {

    /**
     * Produit le rapport de restitution pour le controller reference.
     *
     * @param controllerRef reference non nulle du controller a restituer
     * @return rapport complet, jamais null
     */
    RestitutionReport handle(String controllerRef);
}
