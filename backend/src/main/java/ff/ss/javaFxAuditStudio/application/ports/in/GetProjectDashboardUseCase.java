package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.workbench.ProjectDashboard;

import java.util.List;
import java.util.Optional;

/**
 * Port entrant : cas d'usage de lecture du tableau de bord projet.
 */
public interface GetProjectDashboardUseCase {

    /**
     * Retourne le tableau de bord pour le projet demande.
     *
     * @param projectId identifiant du projet
     * @return Optional.empty() si le projet est inconnu
     */
    Optional<ProjectDashboard> get(String projectId);

    /**
     * Retourne la liste de tous les identifiants de projets connus.
     *
     * @return liste, jamais null
     */
    List<String> listProjects();
}
