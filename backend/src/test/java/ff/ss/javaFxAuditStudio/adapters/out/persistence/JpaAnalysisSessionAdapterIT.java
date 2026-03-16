package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAS-91 — Tests d'integration de l'adapter JPA pour les sessions d'analyse.
 *
 * Note : @DataJpaTest a ete retire de Spring Boot 4.0.3. On utilise
 * @SpringBootTest(webEnvironment = NONE) + profil "test" (H2 in-memory,
 * ddl-auto=create-drop, Flyway desactive) comme alternative.
 *
 * L'adapter est instancie directement avec le repository JPA injecte
 * par le contexte Spring complet.
 *
 * @Transactional garantit le rollback apres chaque test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class JpaAnalysisSessionAdapterIT {

    @Autowired
    private AnalysisSessionRepository repository;

    private JpaAnalysisSessionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaAnalysisSessionAdapter(repository);
    }

    // -------------------------------------------------------------------------
    // JAS-91 — Test 1 : save + findById retrouve la session avec tous les champs
    // -------------------------------------------------------------------------

    @Test
    void save_thenFindById_returnsSessionWithAllFields() {
        String sessionId;
        Instant createdAt;
        AnalysisSession session;
        Optional<AnalysisSession> found;

        sessionId = "sess-jpa-01";
        createdAt = Instant.parse("2026-01-15T10:00:00Z");
        session = new AnalysisSession(
                sessionId,
                "com/example/MyController.java",
                "snippets/MyController.txt",
                AnalysisStatus.CREATED,
                createdAt);

        adapter.save(session);
        found = adapter.findById(sessionId);

        assertThat(found).isPresent();
        assertThat(found.get().sessionId()).isEqualTo(sessionId);
        assertThat(found.get().controllerName()).isEqualTo("com/example/MyController.java");
        assertThat(found.get().sourceSnippetRef()).isEqualTo("snippets/MyController.txt");
        assertThat(found.get().status()).isEqualTo(AnalysisStatus.CREATED);
        assertThat(found.get().createdAt()).isEqualTo(createdAt);
    }

    // -------------------------------------------------------------------------
    // JAS-91 — Test 2 : findById sur ID inexistant → Optional.empty()
    // -------------------------------------------------------------------------

    @Test
    void findById_returnsEmpty_whenSessionDoesNotExist() {
        Optional<AnalysisSession> found;

        found = adapter.findById("id-inexistant-9999");

        assertThat(found).isEmpty();
    }

    // -------------------------------------------------------------------------
    // JAS-91 — Test 3 : transition de statut CREATED → IN_PROGRESS
    // -------------------------------------------------------------------------

    @Test
    void save_updatesStatus_fromCreatedToInProgress() {
        String sessionId;
        AnalysisSession sessionCreated;
        AnalysisSession sessionInProgress;
        Optional<AnalysisSession> found;

        sessionId = "sess-jpa-02";
        sessionCreated = new AnalysisSession(
                sessionId,
                "com/example/TransitionController.java",
                null,
                AnalysisStatus.CREATED,
                Instant.now());

        adapter.save(sessionCreated);

        sessionInProgress = new AnalysisSession(
                sessionId,
                sessionCreated.controllerName(),
                sessionCreated.sourceSnippetRef(),
                AnalysisStatus.IN_PROGRESS,
                sessionCreated.createdAt());
        adapter.save(sessionInProgress);

        found = adapter.findById(sessionId);
        assertThat(found).isPresent();
        assertThat(found.get().status()).isEqualTo(AnalysisStatus.IN_PROGRESS);
    }

    // -------------------------------------------------------------------------
    // JAS-91 — Test 4 : serialisation/deserialisation du champ status (enum → VARCHAR → enum)
    // -------------------------------------------------------------------------

    @Test
    void status_isCorrectlySerializedAndDeserializedAsVarchar() {
        for (AnalysisStatus status : AnalysisStatus.values()) {
            String sessionId;
            AnalysisSession session;
            Optional<AnalysisSession> found;

            sessionId = "sess-status-" + status.name();
            session = new AnalysisSession(
                    sessionId,
                    "com/example/StatusController.java",
                    null,
                    status,
                    Instant.now());

            adapter.save(session);
            found = adapter.findById(sessionId);

            assertThat(found).isPresent();
            assertThat(found.get().status())
                    .as("Statut %s doit survivre au cycle save/findById", status)
                    .isEqualTo(status);
        }
    }
}
