package ff.ss.javaFxAuditStudio.adapters.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;

/**
 * Test de regression sur le corpus sanitise JAS-021.
 *
 * <p>Verifie que chaque handler du corpus produit une suggestion non-nulle et non-vide.
 * La precision semantique reelle est validee manuellement — ce test verifie la structure.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiCorpusRegressionTest {

    private static final String CORPUS_PATH = "/ai-corpus-sanitized/handlers-corpus.json";
    private static final String STUB_API_KEY = "sk-stub-test-key";
    private static final String PROVIDER_CLAUDE = "claude-code";

    @Mock
    private RestClient restClient;

    @Mock(answer = Answers.RETURNS_SELF)
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private ClaudeCodeAiEnrichmentAdapter claudeAdapter;
    private ObjectMapper objectMapper;
    private String expectedControllerRef;

    @BeforeEach
    void setUp() {
        AiEnrichmentProperties.Credentials claudeCreds =
                new AiEnrichmentProperties.Credentials(STUB_API_KEY);
        AiEnrichmentProperties properties = new AiEnrichmentProperties(
                true, PROVIDER_CLAUDE, 10_000L, claudeCreds, null, false, null, null, null, null, null, null);
        PromptTemplateLoader templateLoader = new PromptTemplateLoader();
        objectMapper = new ObjectMapper();

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ClaudeHttpDtos.MessagesResponse.class)).thenAnswer(invocation ->
                new ClaudeHttpDtos.MessagesResponse(
                        List.of(new ClaudeHttpDtos.ContentBlock(
                                "text",
                                "{\"suggestions\":{\"" + expectedControllerRef + "\":\"StubUseCase\"}}")),
                        new ClaudeHttpDtos.Usage(100, 50)));

        claudeAdapter = new ClaudeCodeAiEnrichmentAdapter(properties, templateLoader, restClient, objectMapper);
    }

    @Test
    void should_produce_non_empty_suggestion_for_every_corpus_handler() throws Exception {
        JsonNode root = loadCorpus();
        JsonNode handlers = root.get("handlers");

        assertThat(handlers).isNotNull();
        assertThat(handlers.size()).isEqualTo(20);

        int successCount = 0;
        for (JsonNode handler : handlers) {
            AiEnrichmentResult result = callAdapterForHandler(handler);
            String controllerRef = handler.get("controllerRef").asText();
            String suggestion = result.suggestions().get(controllerRef);
            if (suggestion != null && !suggestion.isBlank()) {
                successCount++;
            }
        }

        assertThat(successCount)
                .as("Toutes les suggestions du corpus doivent etre non-vides")
                .isEqualTo(handlers.size());
    }

    @Test
    void should_return_stub_response_rate_of_100_percent() throws Exception {
        JsonNode handlers = loadCorpus().get("handlers");
        int total = handlers.size();
        int nonEmpty = 0;

        for (JsonNode handler : handlers) {
            AiEnrichmentResult result = callAdapterForHandler(handler);
            String controllerRef = handler.get("controllerRef").asText();
            String suggestion = result.suggestions().get(controllerRef);
            if (suggestion != null && !suggestion.isBlank()) {
                nonEmpty++;
            }
        }

        double rate = (double) nonEmpty / total * 100.0;
        assertThat(rate).as("Taux de reponses non-vides doit etre 100%%").isEqualTo(100.0);
    }

    private AiEnrichmentResult callAdapterForHandler(final JsonNode handler) {
        String controllerRef = handler.get("controllerRef").asText();
        String sanitizedSource = handler.get("sanitizedSource").asText();
        String taskType = handler.get("taskType").asText();
        String promptTemplate = resolveTemplate(taskType);
        expectedControllerRef = controllerRef;

        SanitizedBundle bundle = new SanitizedBundle(
                UUID.randomUUID().toString(),
                controllerRef,
                sanitizedSource,
                sanitizedSource.length(),
                "v1.0",
                null);

        AiEnrichmentRequest request = new AiEnrichmentRequest(
                UUID.randomUUID().toString(),
                bundle,
                TaskType.fromString(taskType),
                promptTemplate);

        return claudeAdapter.call(request);
    }

    private String resolveTemplate(final String taskType) {
        return switch (taskType) {
            case "NAMING" -> "enrichment-naming";
            case "DESCRIPTION" -> "enrichment-description";
            default -> "enrichment-default";
        };
    }

    private JsonNode loadCorpus() throws Exception {
        InputStream stream = getClass().getResourceAsStream(CORPUS_PATH);
        assertThat(stream)
                .as("Corpus introuvable dans le classpath : " + CORPUS_PATH)
                .isNotNull();
        return objectMapper.readTree(stream);
    }
}
