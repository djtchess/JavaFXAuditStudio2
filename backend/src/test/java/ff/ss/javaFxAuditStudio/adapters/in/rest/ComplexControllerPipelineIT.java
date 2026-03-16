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
 * JAS-92 — Test d'integration bout-en-bout du pipeline d'analyse avec ComplexController.java.
 *
 * Meme pattern que JAS-90 (AnalysisPipelineIT) mais avec la fixture complexe.
 * Verifie que le pipeline ne produit pas de 5xx et retourne un finalStatus non nul.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ComplexControllerPipelineIT {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void pipeline_withComplexController_returns200WithNonNullFinalStatus() throws Exception {
        // --- Etape 1 : creation de la session avec la fixture complexe ---
        String createBody;
        MvcResult createResult;
        String createResponse;
        String sessionId;

        createBody = """
                {
                    "sessionName": "ComplexControllerSession",
                    "sourceFilePaths": ["fixtures/ComplexController.java"]
                }
                """;

        createResult = mockMvc.perform(post("/api/v1/analysis/sessions")
                        .contentType(APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        createResponse = createResult.getResponse().getContentAsString();
        assertThat(createResponse).contains("sessionId");

        sessionId = extractJsonField(createResponse, "sessionId");
        assertThat(sessionId).isNotBlank();

        // --- Etape 2 : lancement du pipeline ---
        MvcResult runResult;
        String runResponse;

        // Verification 1 : pas de 5xx
        runResult = mockMvc.perform(post("/api/v1/analysis/sessions/{sessionId}/run", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Verification 2 : finalStatus present et non nul dans le body JSON
        runResponse = runResult.getResponse().getContentAsString();

        assertThat(runResponse)
                .as("Le body /run doit contenir le champ finalStatus")
                .contains("finalStatus");

        // Le champ finalStatus ne doit pas etre null (valeur attendue : COMPLETED ou FAILED)
        assertThat(runResponse)
                .as("finalStatus ne doit pas etre null dans la reponse")
                .doesNotContain("\"finalStatus\":null")
                .doesNotContain("\"finalStatus\": null");
    }

    /**
     * Extraction minimaliste d'un champ scalaire depuis un JSON plat.
     */
    private static String extractJsonField(String json, String fieldName) {
        String marker;
        int start;
        int end;

        marker = "\"" + fieldName + "\":\"";
        start = json.indexOf(marker);
        if (start < 0) {
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
