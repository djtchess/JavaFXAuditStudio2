package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO REST miroir de {@code ProjectDashboard} pour JAS-015.
 */
public record ProjectDashboardResponse(
        String projectId,
        int totalSessions,
        int analysingCount,
        int completedCount,
        Map<String, Long> rulesByCategory,
        long uncertainCount,
        long reclassifiedCount,
        List<String> recommendedLotOrder
) {}
