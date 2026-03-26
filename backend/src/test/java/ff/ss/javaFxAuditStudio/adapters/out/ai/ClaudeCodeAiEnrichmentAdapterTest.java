package ff.ss.javaFxAuditStudio.adapters.out.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de {@link ClaudeCodeAiEnrichmentAdapter}.
 *
 * <p>Le RestClient est mocke via Mockito pour eviter tout appel reseau reel.
 * RequestBodyUriSpec utilise Answers.RETURNS_SELF pour couvrir la chaine fluente
 * sans se heurter aux surcharges de body().
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaudeCodeAiEnrichmentAdapterTest {

    @Mock
    private RestClient restClient;

    @Mock(answer = Answers.RETURNS_SELF)
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private PromptTemplateLoader templateLoader;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        templateLoader = new PromptTemplateLoader();
        objectMapper = new ObjectMapper();
    }

    private AiEnrichmentProperties propertiesWithKey(final String apiKey) {
        return new AiEnrichmentProperties(
                true,
                "claude-code",
                10000L,
                new AiEnrichmentProperties.Credentials(apiKey),
                null,
                false,
                null,
                null,
                null,
                null,
                null);
    }

    private AiEnrichmentProperties propertiesWithKeyAndTokens(
            final String apiKey,
            final Map<ff.ss.javaFxAuditStudio.domain.ai.TaskType, Integer> maxTokensByTask) {
        return new AiEnrichmentProperties(
                true,
                "claude-code",
                10000L,
                new AiEnrichmentProperties.Credentials(apiKey),
                null,
                false,
                null,
                null,
                maxTokensByTask,
                null,
                null);
    }

    private AiEnrichmentProperties propertiesWithKeyAndResponseLimit(
            final String apiKey,
            final int maxResponseSizeBytes) {
        return new AiEnrichmentProperties(
                true,
                "claude-code",
                10000L,
                new AiEnrichmentProperties.Credentials(apiKey),
                null,
                false,
                null,
                null,
                null,
                maxResponseSizeBytes,
                null);
    }

    private AiEnrichmentRequest buildRequest() {
        SanitizedBundle bundle = new SanitizedBundle(
                "bundle-test",
                "MyController",
                "@FXML void onSave() { service.save(); }",
                42,
                "1.0");
        return new AiEnrichmentRequest(
                UUID.randomUUID().toString(),
                bundle,
                TaskType.NAMING,
                "enrichment-naming");
    }

    private void stubRestClientChain(final ClaudeHttpDtos.MessagesResponse mockedResponse) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ClaudeHttpDtos.MessagesResponse.class)).thenReturn(mockedResponse);
    }

    @Test
    void should_return_suggestions_from_valid_json_response() {
        // Reponse Claude nominale avec JSON bien forme
        ClaudeHttpDtos.MessagesResponse response = new ClaudeHttpDtos.MessagesResponse(
                List.of(new ClaudeHttpDtos.ContentBlock(
                        "text",
                        "{\"suggestions\": {\"MyController\": \"SavePatientUseCase\"}}")),
                new ClaudeHttpDtos.Usage(100, 50));
        stubRestClientChain(response);

        ClaudeCodeAiEnrichmentAdapter adapter = new ClaudeCodeAiEnrichmentAdapter(
                propertiesWithKey("sk-test-key"),
                templateLoader,
                restClient,
                objectMapper);

        AiEnrichmentResult result = adapter.call(buildRequest());

        assertThat(result.degraded()).isFalse();
        assertThat(result.suggestions()).containsKey("MyController");
        assertThat(result.suggestions().get("MyController")).isEqualTo("SavePatientUseCase");
        assertThat(result.provider()).isEqualTo(LlmProvider.CLAUDE_CODE);
        assertThat(result.tokensUsed()).isEqualTo(150);
    }

    @Test
    void should_return_raw_text_as_suggestion_when_response_is_not_json() {
        // Reponse en texte brut (sans JSON)
        ClaudeHttpDtos.MessagesResponse response = new ClaudeHttpDtos.MessagesResponse(
                List.of(new ClaudeHttpDtos.ContentBlock("text", "DeleteUserUseCase")),
                new ClaudeHttpDtos.Usage(80, 20));
        stubRestClientChain(response);

        ClaudeCodeAiEnrichmentAdapter adapter = new ClaudeCodeAiEnrichmentAdapter(
                propertiesWithKey("sk-test-key"),
                templateLoader,
                restClient,
                objectMapper);

        AiEnrichmentResult result = adapter.call(buildRequest());

        assertThat(result.degraded()).isFalse();
        assertThat(result.suggestions()).containsEntry("MyController", "DeleteUserUseCase");
    }

    @Test
    void should_return_degraded_result_when_content_list_is_empty() {
        // Reponse Claude avec liste content vide
        ClaudeHttpDtos.MessagesResponse response = new ClaudeHttpDtos.MessagesResponse(
                List.of(),
                new ClaudeHttpDtos.Usage(50, 0));
        stubRestClientChain(response);

        ClaudeCodeAiEnrichmentAdapter adapter = new ClaudeCodeAiEnrichmentAdapter(
                propertiesWithKey("sk-test-key"),
                templateLoader,
                restClient,
                objectMapper);

        AiEnrichmentResult result = adapter.call(buildRequest());

        assertThat(result.degraded()).isTrue();
        assertThat(result.degradationReason()).contains("vide");
    }

    @Test
    void should_throw_illegal_state_when_api_key_is_absent() {
        // Credential absent — doit lever IllegalStateException avant tout appel HTTP
        ClaudeCodeAiEnrichmentAdapter adapter = new ClaudeCodeAiEnrichmentAdapter(
                propertiesWithKey(""),
                templateLoader,
                restClient,
                objectMapper);

        assertThatThrownBy(() -> adapter.call(buildRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLAUDE_API_KEY");
    }

    @Test
    void should_return_degraded_when_response_is_null() {
        // Cas ou RestClient retourne null (corps vide)
        stubRestClientChain(null);

        ClaudeCodeAiEnrichmentAdapter adapter = new ClaudeCodeAiEnrichmentAdapter(
                propertiesWithKey("sk-test-key"),
                templateLoader,
                restClient,
                objectMapper);

        AiEnrichmentResult result = adapter.call(buildRequest());

        assertThat(result.degraded()).isTrue();
    }

    @Test
    void should_use_configured_max_tokens_for_request_task_type() {
        ClaudeHttpDtos.MessagesResponse response = new ClaudeHttpDtos.MessagesResponse(
                List.of(new ClaudeHttpDtos.ContentBlock("text", "{\"suggestions\": {\"MyController\": \"SavePatientUseCase\"}}")),
                new ClaudeHttpDtos.Usage(100, 50));
        stubRestClientChain(response);

        ClaudeCodeAiEnrichmentAdapter adapter = new ClaudeCodeAiEnrichmentAdapter(
                propertiesWithKeyAndTokens("sk-test-key", Map.of(TaskType.SPRING_BOOT_GENERATION, 4096)),
                templateLoader,
                restClient,
                objectMapper);

        SanitizedBundle bundle = new SanitizedBundle(
                "bundle-test",
                "MyController",
                "@FXML void onSave() { service.save(); }",
                42,
                "1.0");
        AiEnrichmentRequest request = new AiEnrichmentRequest(
                UUID.randomUUID().toString(),
                bundle,
                TaskType.SPRING_BOOT_GENERATION,
                "spring-boot-generation");

        adapter.call(request);

        org.mockito.ArgumentCaptor<ClaudeHttpDtos.MessagesRequest> captor =
                org.mockito.ArgumentCaptor.forClass(ClaudeHttpDtos.MessagesRequest.class);
        org.mockito.Mockito.verify(requestBodyUriSpec).body(captor.capture());
        assertThat(captor.getValue().maxTokens()).isEqualTo(4096);
    }

    @Test
    void should_return_degraded_when_response_exceeds_max_response_size() {
        String oversized = "a".repeat(32);
        ClaudeHttpDtos.MessagesResponse response = new ClaudeHttpDtos.MessagesResponse(
                List.of(new ClaudeHttpDtos.ContentBlock("text", oversized)),
                new ClaudeHttpDtos.Usage(100, 50));
        stubRestClientChain(response);

        ClaudeCodeAiEnrichmentAdapter adapter = new ClaudeCodeAiEnrichmentAdapter(
                propertiesWithKeyAndResponseLimit("sk-test-key", 16),
                templateLoader,
                restClient,
                objectMapper);

        AiEnrichmentResult result = adapter.call(buildRequest());

        assertThat(result.degraded()).isTrue();
        assertThat(result.provider()).isEqualTo(LlmProvider.CLAUDE_CODE);
        assertThat(result.degradationReason()).contains("tronquee").contains("16");
        assertThat(result.suggestions()).isEmpty();
    }
}
