package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatusTransition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class JpaAnalysisSessionStatusHistoryAdapterIT {

    @Autowired
    private AnalysisSessionStatusHistoryRepository repository;

    private JpaAnalysisSessionStatusHistoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaAnalysisSessionStatusHistoryAdapter(repository);
    }

    @Test
    void save_thenFindBySessionId_returnsOrderedHistory() {
        adapter.save(new AnalysisStatusTransition(
                "session-1",
                AnalysisStatus.CREATED,
                Instant.parse("2026-03-30T10:00:00Z")));
        adapter.save(new AnalysisStatusTransition(
                "session-1",
                AnalysisStatus.INGESTING,
                Instant.parse("2026-03-30T10:01:00Z")));

        List<AnalysisStatusTransition> history = adapter.findBySessionId("session-1");

        assertThat(history)
                .extracting(AnalysisStatusTransition::status)
                .containsExactly(AnalysisStatus.CREATED, AnalysisStatus.INGESTING);
    }

    @Test
    void findLatestTransitionAt_returnsMostRecentTimestamp() {
        adapter.save(new AnalysisStatusTransition(
                "session-1",
                AnalysisStatus.CREATED,
                Instant.parse("2026-03-30T10:00:00Z")));
        adapter.save(new AnalysisStatusTransition(
                "session-2",
                AnalysisStatus.REPORTING,
                Instant.parse("2026-03-30T10:05:00Z")));

        assertThat(adapter.findLatestTransitionAt())
                .contains(Instant.parse("2026-03-30T10:05:00Z"));
    }
}
