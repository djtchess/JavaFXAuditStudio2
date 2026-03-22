package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.workbench.ProjectDashboard;

import java.util.List;
import java.util.Optional;

/**
 * Port sortant : agregation des metriques de progression par projet.
 * Le projectId correspond au controllerName commun aux sessions.
 */
public interface ProjectDashboardPort {

    /**
     * Calcule le tableau de bord pour un projet donne.
     *
     * @param projectId identifiant du projet (= controllerName des sessions)
     * @return Optional.empty() si aucune session n'existe pour ce projet
     */
    Optional<ProjectDashboard> computeDashboard(String projectId);

    /**
     * Retourne la liste de tous les projectIds connus (controllerNames distincts).
     *
     * @return liste ordonnee alphabetiquement, jamais null
     */
    List<String> findAllProjectIds();
}
