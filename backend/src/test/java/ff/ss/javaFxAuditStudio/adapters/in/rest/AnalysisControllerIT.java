package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.application.ports.in.AnalysisOrchestrationUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import ff.ss.javaFxAuditStudio.domain.workbench.OrchestratedAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JAS-89 — Tests d'integration du controller REST AnalysisController.
 *
 * Utilise @SpringBootTest(webEnvironment = MOCK) + MockMvcBuilders.webAppContextSetup
 * car @WebMvcTest a ete retire de Spring Boot 4.0.3.
 *
 * Les ports et use cases sont bouchonnes via @MockitoBean (spring-test 7+).
 * Le profil "test" active H2 + ddl-auto=create-drop, evitant toute connexion PostgreSQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AnalysisControllerIT {

    @Autowired
    private WebApplicationContext wac;

    // --- Ports et use cases bouchonnes ---

    @MockitoBean
    private AnalysisSessionPort analysisSessionPort;

    @MockitoBean
    private AnalysisOrchestrationUseCase analysisOrchestrationUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // -------------------------------------------------------------------------
    // JAS-89 — Test 1 : POST /api/v1/analysis/sessions avec body valide → 201
    // -------------------------------------------------------------------------

    @Test
    void submitSession_returns201_withSessionIdNonNull() throws Exception {
        // Arrange
        String requestBody;
        String responseBody;

        requestBody = """
                {
                    "sessionName": "MonController",
                    "sourceFilePaths": ["src/main/java/com/example/MyController.java"]
                }
                """;

        when(analysisSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act + Assert
        responseBody = mockMvc.perform(post("/api/v1/analysis/sessions")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).contains("sessionId");
    }

    // -------------------------------------------------------------------------
    // JAS-89 — Test 2 : POST /run avec sessionId inexistant → 404
    // -------------------------------------------------------------------------

    @Test
    void runPipeline_returns404_whenSessionNotFound() throws Exception {
        String sessionId;

        sessionId = "session-introuvable-it";
        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/analysis/sessions/{sessionId}/run", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // JAS-89 — Test 3 : POST /run avec session en statut IN_PROGRESS → 409
    // -------------------------------------------------------------------------

    @Test
    void runPipeline_returns409_whenSessionInProgress() throws Exception {
        String sessionId;
        AnalysisSession sessionEnCours;

        sessionId = "session-en-cours-it";
        sessionEnCours = new AnalysisSession(
                sessionId,
                "com/example/MyController.java",
                null,
                AnalysisStatus.IN_PROGRESS,
                Instant.now());

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(sessionEnCours));

        mockMvc.perform(post("/api/v1/analysis/sessions/{sessionId}/run", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------------------------------
    // JAS-89 — Test complementaire : POST /run avec session CREATED → 200
    // -------------------------------------------------------------------------

    @Test
    void runPipeline_returns200_whenSessionCreated() throws Exception {
        String sessionId;
        AnalysisSession sessionCreee;
        OrchestratedAnalysisResult orchestrationResult;

        sessionId = "session-prete-it";
        sessionCreee = new AnalysisSession(
                sessionId,
                "com/example/MyController.java",
                null,
                AnalysisStatus.CREATED,
                Instant.now());

        orchestrationResult = new OrchestratedAnalysisResult(
                sessionId,
                AnalysisStatus.COMPLETED,
                null, null, null, null, null,
                List.of());

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(sessionCreee));
        when(analysisOrchestrationUseCase.orchestrate(sessionId)).thenReturn(orchestrationResult);

        // Le mapper est le bean reel du contexte Spring — on s'assure que le resultat est mappe.
        // Le bean OrchestratedAnalysisResultResponseMapper est reel (non mocke) et appellera
        // les sous-mappers reels. On verifie uniquement le statut HTTP et la presence du champ.
        mockMvc.perform(post("/api/v1/analysis/sessions/{sessionId}/run", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.finalStatus").value("COMPLETED"));
    }
}
