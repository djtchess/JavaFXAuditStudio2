package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.application.ports.out.ProjectDashboardPort;
import ff.ss.javaFxAuditStudio.domain.workbench.ProjectDashboard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter JPA implementant {@link ProjectDashboardPort}.
 * Construit le tableau de bord depuis des requetes JPQL agregees
 * sans charger d'entites en memoire.
 */
public class JpaProjectDashboardAdapter implements ProjectDashboardPort {

    private static final List<String> ANALYSING_STATUSES = List.of(
            "PENDING",
            "IN_PROGRESS",
            "RUNNING",
            "INGESTING",
            "CARTOGRAPHING",
            "CLASSIFYING",
            "PLANNING",
            "GENERATING",
            "REPORTING");

    private final AnalysisSessionRepository sessionRepository;
    private final BusinessRuleRepository businessRuleRepository;
    private final RuleClassificationAuditRepository auditRepository;

    public JpaProjectDashboardAdapter(
            final AnalysisSessionRepository sessionRepository,
            final BusinessRuleRepository businessRuleRepository,
            final RuleClassificationAuditRepository auditRepository) {
        this.sessionRepository = sessionRepository;
        this.businessRuleRepository = businessRuleRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    public Optional<ProjectDashboard> computeDashboard(final String projectId) {
        long total = sessionRepository.countByControllerName(projectId);
        if (total == 0) {
            return Optional.empty();
        }
        long analysing = sessionRepository.countByControllerNameAndStatusIn(projectId, ANALYSING_STATUSES);
        long completed = sessionRepository.countByControllerNameAndStatus(projectId, "COMPLETED");
        Map<String, Long> rulesByCategory = buildRulesByCategory(projectId);
        long uncertain = businessRuleRepository.countUncertainRulesForProject(projectId);
        long reclassified = auditRepository.countReclassificationsForProject(projectId);
        List<String> lotOrder = buildRecommendedLotOrder(rulesByCategory);

        return Optional.of(new ProjectDashboard(
                projectId,
                (int) total,
                (int) analysing,
                (int) completed,
                rulesByCategory,
                uncertain,
                reclassified,
                lotOrder));
    }

    @Override
    public List<String> findAllProjectIds() {
        return sessionRepository.findDistinctControllerNames();
    }

    private Map<String, Long> buildRulesByCategory(final String projectId) {
        List<Object[]> rows = businessRuleRepository.countRulesByCategoryForProject(projectId);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String category = row[0] != null ? row[0].toString() : "UNKNOWN";
            Long count = ((Number) row[1]).longValue();
            result.put(category, count);
        }
        return result;
    }

    private List<String> buildRecommendedLotOrder(final Map<String, Long> rulesByCategory) {
        return rulesByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
    }
}
