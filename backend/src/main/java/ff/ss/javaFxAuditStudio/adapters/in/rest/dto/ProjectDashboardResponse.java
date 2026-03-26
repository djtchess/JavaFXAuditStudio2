package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * DTO REST miroir de {@code ProjectDashboard} pour JAS-015.
 */
@Schema(description = "Metriques de progression pour un projet")
public record ProjectDashboardResponse(
        @Schema(description = "Identifiant du projet")
        String projectId,
        @Schema(description = "Nombre total de sessions d'analyse")
        int totalSessions,
        @Schema(description = "Sessions en cours d'analyse")
        int analysingCount,
        @Schema(description = "Sessions terminees")
        int completedCount,
        @Schema(description = "Repartition des regles par categorie")
        Map<String, Long> rulesByCategory,
        @Schema(description = "Regles avec classification incertaine")
        long uncertainCount,
        @Schema(description = "Regles reclassifiees manuellement")
        long reclassifiedCount,
        @Schema(description = "Ordre recommande de traitement par volume de regles")
        List<String> recommendedLotOrder
) {}
