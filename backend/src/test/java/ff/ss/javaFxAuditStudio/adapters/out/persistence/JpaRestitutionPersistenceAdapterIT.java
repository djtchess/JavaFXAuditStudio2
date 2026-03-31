package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.domain.restitution.ConfidenceLevel;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionSummary;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class JpaRestitutionPersistenceAdapterIT {

    @Autowired
    private AnalysisSessionRepository analysisSessionRepository;

    @Autowired
    private RestitutionReportRepository restitutionReportRepository;

    private JpaRestitutionPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaRestitutionPersistenceAdapter(restitutionReportRepository);
    }

    @Test
    void save_thenFindBySessionId_preservesMarkdown() {
        analysisSessionRepository.save(new AnalysisSessionEntity(
                "session-report",
                "Audit report",
                "com/example/MyController.java",
                null,
                AnalysisStatus.COMPLETED.name(),
                Instant.parse("2026-03-30T10:15:00Z")));

        RestitutionReport report = new RestitutionReport(
                new RestitutionSummary(
                        "com/example/MyController.java",
                        2,
                        0,
                        1,
                        0,
                        ConfidenceLevel.HIGH,
                        false),
                List.of(),
                List.of("Manual review"),
                List.of("Use case extracted"),
                "# Restitution\n\n## Synthese");

        adapter.save("session-report", report);
        Optional<RestitutionReport> found = adapter.findBySessionId("session-report");

        assertThat(found).isPresent();
        assertThat(found.get().markdown()).isEqualTo("# Restitution\n\n## Synthese");
        assertThat(found.get().findings()).containsExactly("Use case extracted");
    }
}
