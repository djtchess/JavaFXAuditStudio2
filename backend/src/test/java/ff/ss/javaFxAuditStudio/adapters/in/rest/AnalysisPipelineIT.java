package ff.ss.javaFxAuditStudio.adapters.in.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'integration bout-en-bout du pipeline d'analyse avec les fixtures simples.
 * Verifie que le pipeline complet expose un contenu metier exploitable sur les endpoints REST.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AnalysisPipelineIT {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void pipeline_withSampleFixtures_returnsRichBusinessOutputs() throws Exception {
        String sessionId = createSession("SampleControllerSession", "SampleController.java", "SampleView.fxml");

        mockMvc.perform(post("/api/v1/analysis/sessions/{sessionId}/run", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.finalStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.cartography.components.length()").value(greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.cartography.components[*].fxId").value(hasItems(
                        "nameField",
                        "submitButton",
                        "resetButton",
                        "statusLabel")))
                .andExpect(jsonPath("$.cartography.handlers[*].methodName").value(hasItems(
                        "handleSubmit",
                        "handleReset",
                        "handleClose")))
                .andExpect(jsonPath("$.classification.ruleCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.classification.rules[*].extractionCandidate").value(hasItems(
                        "VIEW_MODEL",
                        "USE_CASE")))
                .andExpect(jsonPath("$.classification.dependencies[*].target").value(hasItems(
                        "UserService",
                        "NotificationService")))
                .andExpect(jsonPath("$.migrationPlan.lots.length()").value(greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.generationResult.artifacts.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.generationResult.artifacts[*].type").value(hasItems(
                        "CONTROLLER_SLIM",
                        "VIEW_MODEL",
                        "USE_CASE")))
                .andExpect(jsonPath("$.restitutionReport.markdown").value(containsString("## Lots")))
                .andExpect(jsonPath("$.restitutionReport.markdown").value(containsString("## Artefacts")));

        mockMvc.perform(get("/api/v1/analysis/sessions/{sessionId}", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.controllerRef").value(fixturePath("SampleController.java")))
                .andExpect(jsonPath("$.sourceSnippetRef").value(fixturePath("SampleView.fxml")));

        mockMvc.perform(get("/api/v1/analysis/sessions/{sessionId}/classification", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.rules.length()").value(greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.rules[*].description").value(hasItems(
                        containsString("handler handleSubmit"),
                        containsString("Champ FXML Button submitButton"))));

        mockMvc.perform(get("/api/v1/analysis/sessions/{sessionId}/artifacts", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifacts[*].className").value(hasItems(
                        "SampleSlimController",
                        "SampleViewModel",
                        "SampleUseCase")));

        mockMvc.perform(get("/api/v1/analysis/sessions/{sessionId}/report", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifactCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.unknowns.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.markdown").value(containsString("# Restitution")));
    }

    private String createSession(
            final String sessionName,
            final String controllerFixture,
            final String fxmlFixture) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "sessionName", sessionName,
                "sourceFilePaths", List.of(
                        fixturePath(controllerFixture),
                        fixturePath(fxmlFixture))));

        MvcResult createResult = mockMvc.perform(post("/api/v1/analysis/sessions")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andReturn();
        JsonNode response = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String sessionId = response.path("sessionId").asText();

        assertThat(sessionId).isNotBlank();
        return sessionId;
    }

    private static String fixturePath(final String fixtureName) {
        try {
            return Path.of(Objects.requireNonNull(
                    AnalysisPipelineIT.class.getClassLoader().getResource("fixtures/" + fixtureName),
                    "Fixture introuvable: " + fixtureName).toURI()).toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible de resoudre la fixture " + fixtureName, exception);
        }
    }
}
