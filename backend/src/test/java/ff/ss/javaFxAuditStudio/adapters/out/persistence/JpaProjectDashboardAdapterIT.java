package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.domain.workbench.ProjectDashboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TU JAS-015 — JpaProjectDashboardAdapter avec H2 in-memory.
 * Utilise @SpringBootTest(webEnvironment=NONE) + profil "test" comme les autres IT JPA du projet.
 * @Transactional garantit le rollback apres chaque test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class JpaProjectDashboardAdapterIT {

    @Autowired
    private AnalysisSessionRepository sessionRepository;

    @Autowired
    private BusinessRuleRepository businessRuleRepository;

    @Autowired
    private RuleClassificationAuditRepository auditRepository;

    @Autowired
    private ClassificationResultRepository classificationResultRepository;

    private JpaProjectDashboardAdapter adapter;

    private static final String PROJECT_ID = "com/example/DashboardController.java";

    @BeforeEach
    void setUp() {
        adapter = new JpaProjectDashboardAdapter(sessionRepository, businessRuleRepository, auditRepository);
    }

    @Test
    void computeDashboard_returnsEmpty_whenNoSessionExists() {
        Optional<ProjectDashboard> result = adapter.computeDashboard("projet-inconnu");

        assertThat(result).isEmpty();
    }

    @Test
    void computeDashboard_returnsTotalAndStatusCounts() {
        insertSession("s1", PROJECT_ID, "IN_PROGRESS");
        insertSession("s2", PROJECT_ID, "COMPLETED");
        insertSession("s3", PROJECT_ID, "COMPLETED");

        Optional<ProjectDashboard> result = adapter.computeDashboard(PROJECT_ID);

        assertThat(result).isPresent();
        ProjectDashboard d = result.get();
        assertThat(d.projectId()).isEqualTo(PROJECT_ID);
        assertThat(d.totalSessions()).isEqualTo(3);
        assertThat(d.analysingCount()).isEqualTo(1);
        assertThat(d.completedCount()).isEqualTo(2);
    }

    @Test
    void computeDashboard_countsUncertainRules() {
        insertSession("s4", PROJECT_ID, "COMPLETED");
        ClassificationResultEntity cr = insertClassificationResult("s4");
        insertBusinessRule(cr, "UI", false);
        insertBusinessRule(cr, "BUSINESS", true);
        insertBusinessRule(cr, "BUSINESS", true);

        Optional<ProjectDashboard> result = adapter.computeDashboard(PROJECT_ID);

        assertThat(result).isPresent();
        assertThat(result.get().uncertainCount()).isEqualTo(2L);
    }

    @Test
    void computeDashboard_countsReclassifications() {
        insertSession("s5", PROJECT_ID, "COMPLETED");
        insertAuditEntry("s5", "RG-001");
        insertAuditEntry("s5", "RG-002");

        Optional<ProjectDashboard> result = adapter.computeDashboard(PROJECT_ID);

        assertThat(result).isPresent();
        assertThat(result.get().reclassifiedCount()).isEqualTo(2L);
    }

    @Test
    void computeDashboard_buildsRecommendedLotOrderByVolumeDesc() {
        insertSession("s6", PROJECT_ID, "COMPLETED");
        ClassificationResultEntity cr = insertClassificationResult("s6");
        insertBusinessRule(cr, "UI", false);
        insertBusinessRule(cr, "UI", false);
        insertBusinessRule(cr, "BUSINESS", false);

        Optional<ProjectDashboard> result = adapter.computeDashboard(PROJECT_ID);

        assertThat(result).isPresent();
        assertThat(result.get().recommendedLotOrder()).first().isEqualTo("UI");
    }

    @Test
    void findAllProjectIds_returnsDistinctControllerNames() {
        insertSession("s7", "ControllerA", "CREATED");
        insertSession("s8", "ControllerA", "COMPLETED");
        insertSession("s9", "ControllerB", "CREATED");

        java.util.List<String> ids = adapter.findAllProjectIds();

        assertThat(ids).contains("ControllerA", "ControllerB");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void insertSession(final String sessionId, final String controllerName, final String status) {
        sessionRepository.save(new AnalysisSessionEntity(
                sessionId, controllerName, null, status, Instant.now()));
    }

    private ClassificationResultEntity insertClassificationResult(final String sessionId) {
        ClassificationResultEntity cr = new ClassificationResultEntity(sessionId, null, Instant.now());
        return classificationResultRepository.save(cr);
    }

    private void insertBusinessRule(
            final ClassificationResultEntity cr,
            final String responsibilityClass,
            final boolean uncertain) {
        businessRuleRepository.save(new BusinessRuleEntity(
                cr, "RG-" + UUID.randomUUID(), "desc", null, 0, responsibilityClass, null, uncertain));
    }

    private void insertAuditEntry(final String analysisId, final String ruleId) {
        auditRepository.save(new RuleClassificationAuditEntity(
                UUID.randomUUID().toString(), analysisId, ruleId,
                "UI", "BUSINESS", "raison test", Instant.now()));
    }
}
