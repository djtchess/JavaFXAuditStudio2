package ff.ss.javaFxAuditStudio.adapters.in.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JAS-90 — Test d'integration bout-en-bout du pipeline d'analyse avec SampleController.java.
 *
 * Enchaîne POST /sessions puis POST /sessions/{id}/run sur le contexte Spring complet
 * (H2 in-memory, adapteurs reels, aucun mock).
 *
 * La fixture SampleController.java est passee comme chemin de reference de session.
 * Si le pipeline echoue pour une raison filesystem, la reponse doit quand meme
 * etre 200 avec un finalStatus dans le body (COMPLETED ou FAILED), jamais 5xx.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AnalysisPipelineIT {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void pipeline_withSampleController_returns200WithFinalStatus() throws Exception {
        // --- Etape 1 : creation de la session ---
        String createBody;
        MvcResult createResult;
        String createResponse;
        String sessionId;

        createBody = """
                {
                    "sessionName": "SampleControllerSession",
                    "sourceFilePaths": ["fixtures/SampleController.java"]
                }
                """;

        createResult = mockMvc.perform(post("/api/v1/analysis/sessions")
                        .contentType(APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        createResponse = createResult.getResponse().getContentAsString();
        assertThat(createResponse).contains("sessionId");

        // Extraction naive du sessionId depuis le JSON retourne
        sessionId = extractJsonField(createResponse, "sessionId");
        assertThat(sessionId).isNotBlank();

        // --- Etape 2 : lancement du pipeline ---
        MvcResult runResult;
        String runResponse;

        runResult = mockMvc.perform(post("/api/v1/analysis/sessions/{sessionId}/run", sessionId)
                        .contentType(APPLICATION_JSON))
                // Accepte 200 uniquement — jamais de 5xx meme si le pipeline echoue en interne
                .andExpect(status().isOk())
                .andReturn();

        // --- Etape 3 : verification du body ---
        runResponse = runResult.getResponse().getContentAsString();

        assertThat(runResponse).as("Le body /run doit contenir finalStatus").contains("finalStatus");

        // Le finalStatus doit etre COMPLETED ou FAILED (le pipeline peut echouer
        // sur les fichiers fixtures qui ne sont pas de vrais fichiers filesystem)
        boolean isCompleted = runResponse.contains("\"finalStatus\":\"COMPLETED\"")
                || runResponse.contains("\"finalStatus\": \"COMPLETED\"");
        boolean isFailed = runResponse.contains("\"finalStatus\":\"FAILED\"")
                || runResponse.contains("\"finalStatus\": \"FAILED\"");

        assertThat(isCompleted || isFailed)
                .as("finalStatus doit etre COMPLETED ou FAILED, body obtenu : %s", runResponse)
                .isTrue();
    }

    /**
     * Extraction minimaliste d'un champ scalaire depuis un JSON plat.
     * Evite une dependance Jackson supplementaire dans le corps du test.
     */
    private static String extractJsonField(String json, String fieldName) {
        String marker;
        int start;
        int end;

        marker = "\"" + fieldName + "\":\"";
        start = json.indexOf(marker);
        if (start < 0) {
            // Tente avec espace apres les deux-points
            marker = "\"" + fieldName + "\": \"";
            start = json.indexOf(marker);
        }
        if (start < 0) {
            return "";
        }
        start += marker.length();
        end = json.indexOf('"', start);
        if (end < 0) {
            return "";
        }
        return json.substring(start, end);
    }
}
