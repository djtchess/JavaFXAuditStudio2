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

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JAS-92 — Test d'integration bout-en-bout du pipeline avec les fixtures complexes.
 *
 * <p>Verifie la richesse du contenu metier produit par le pipeline sur un controller
 * plus dense qu'un simple smoke test HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ComplexControllerPipelineIT {

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
    void pipeline_withComplexFixtures_returnsRichAndTraceableBusinessContent() throws Exception {
        String sessionId = createSession("ComplexControllerSession", "ComplexController.java", "ComplexView.fxml");
        JsonNode runPayload = postJson("/api/v1/analysis/sessions/{sessionId}/run", sessionId);
        JsonNode cartographyPayload = getJson("/api/v1/analysis/sessions/{sessionId}/cartography", sessionId);
        JsonNode classificationPayload = getJson("/api/v1/analysis/sessions/{sessionId}/classification", sessionId);
        JsonNode planPayload = getJson("/api/v1/analysis/sessions/{sessionId}/plan", sessionId);
        JsonNode artifactsPayload = getJson("/api/v1/analysis/sessions/{sessionId}/artifacts", sessionId);
        JsonNode reportPayload = getJson("/api/v1/analysis/sessions/{sessionId}/report", sessionId);

        assertThat(runPayload.path("finalStatus").asText()).isEqualTo("COMPLETED");

        assertThat(cartographyPayload.path("components").size()).isGreaterThanOrEqualTo(6);
        assertThat(cartographyPayload.path("handlers").size()).isGreaterThanOrEqualTo(8);
        assertThat(extractTexts(cartographyPayload.path("components"), "fxId"))
                .contains("searchField", "searchButton", "categoryCombo", "resultTable", "infoLabel");

        int totalRules = classificationPayload.path("ruleCount").asInt()
                + classificationPayload.path("uncertainCount").asInt();
        assertThat(totalRules).isGreaterThanOrEqualTo(8);
        assertThat(classificationPayload.path("excludedLifecycleMethodsCount").asInt()).isEqualTo(1);
        assertThat(extractTexts(classificationPayload.path("dependencies"), "kind"))
                .contains("SHARED_SERVICE");

        assertThat(planPayload.path("compilable").asBoolean()).isTrue();
        assertThat(planPayload.path("lots").size()).isGreaterThanOrEqualTo(4);
        assertThat(countExtractionCandidates(planPayload.path("lots"))).isGreaterThanOrEqualTo(7);

        List<String> artifactTypes = extractTexts(artifactsPayload.path("artifacts"), "type");
        assertThat(artifactsPayload.path("artifacts").size()).isGreaterThanOrEqualTo(4);
        assertThat(artifactTypes.stream().distinct().count()).isGreaterThanOrEqualTo(3);
        assertThat(artifactTypes).contains("CONTROLLER_SLIM", "VIEW_MODEL", "USE_CASE");

        assertThat(reportPayload.path("isActionable").asBoolean()).isTrue();
        assertThat(reportPayload.path("artifactCount").asInt()).isGreaterThanOrEqualTo(4);
        assertThat(reportPayload.path("markdown").asText())
                .contains("# Restitution", "## Lots", "## Artefacts");
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
        JsonNode createPayload = postJson("/api/v1/analysis/sessions", requestBody, null);

        assertThat(createPayload.path("status").asText()).isEqualTo("CREATED");
        return createPayload.path("sessionId").asText();
    }

    private JsonNode getJson(final String pathTemplate, final String sessionId) throws Exception {
        MvcResult result = mockMvc.perform(get(pathTemplate, sessionId).contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result);
    }

    private JsonNode postJson(
            final String pathTemplate,
            final String sessionId) throws Exception {
        MvcResult result = mockMvc.perform(post(pathTemplate, sessionId).contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result);
    }

    private JsonNode postJson(
            final String pathTemplate,
            final String requestBody,
            final String ignoredSessionId) throws Exception {
        MvcResult result = mockMvc.perform(post(pathTemplate)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result);
    }

    private JsonNode readJson(final MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private List<String> extractTexts(final JsonNode arrayNode, final String fieldName) {
        List<String> values = new ArrayList<>();

        for (JsonNode node : arrayNode) {
            values.add(node.path(fieldName).asText());
        }
        return values;
    }

    private int countExtractionCandidates(final JsonNode lotsNode) {
        int total = 0;

        for (JsonNode lot : lotsNode) {
            total += lot.path("extractionCandidates").size();
        }
        return total;
    }

    private static String fixturePath(final String fixtureName) {
        try {
            return Path.of(Objects.requireNonNull(
                    ComplexControllerPipelineIT.class.getClassLoader().getResource("fixtures/" + fixtureName),
                    "Fixture introuvable: " + fixtureName).toURI()).toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible de resoudre la fixture " + fixtureName, exception);
        }
    }
}
