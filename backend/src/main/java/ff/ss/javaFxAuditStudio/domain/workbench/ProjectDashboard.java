package ff.ss.javaFxAuditStudio.domain.workbench;

import java.util.List;
import java.util.Map;

/**
 * Agregat domaine representant le tableau de bord metrique d'un projet.
 * Un projet regroupe toutes les sessions partageant le meme controllerName.
 * <p>
 * Pur : aucune dependance Spring, JPA ou framework technique.
 */
public record ProjectDashboard(
        String projectId,
        int totalSessions,
        int analysingCount,
        int completedCount,
        Map<String, Long> rulesByCategory,
        long uncertainCount,
        long reclassifiedCount,
        List<String> recommendedLotOrder
) {}
