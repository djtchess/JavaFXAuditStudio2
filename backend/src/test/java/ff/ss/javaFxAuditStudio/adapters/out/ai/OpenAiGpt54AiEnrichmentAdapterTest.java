package ff.ss.javaFxAuditStudio.adapters.out.ai;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;

import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenAiGpt54AiEnrichmentAdapterTest {

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
                "openai-gpt54",
                10000L,
                null,
                new AiEnrichmentProperties.Credentials(apiKey),
                false,
                null,
                null,
                null,
                null,
                null);
    }

    private AiEnrichmentProperties propertiesWithKeyAndTokens(
            final String apiKey,
            final Map<TaskType, Integer> maxTokensByTask) {
        return new AiEnrichmentProperties(
                true,
                "openai-gpt54",
                10000L,
                null,
                new AiEnrichmentProperties.Credentials(apiKey),
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
                "openai-gpt54",
                10000L,
                null,
                new AiEnrichmentProperties.Credentials(apiKey),
                false,
                null,
                null,
                null,
                maxResponseSizeBytes,
                null);
    }

    private AiEnrichmentRequest buildRequest(final TaskType taskType, final String promptTemplate) {
        SanitizedBundle bundle = new SanitizedBundle(
                "bundle-test",
                "MyController",
                "@FXML void onSave() { service.save(); }",
                42,
                "1.0");
        return new AiEnrichmentRequest(
                UUID.randomUUID().toString(),
                bundle,
                taskType,
                promptTemplate);
    }

    private void stubRestClientChain(final OpenAiHttpDtos.ChatResponse mockedResponse) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OpenAiHttpDtos.ChatResponse.class)).thenReturn(mockedResponse);
    }

    @Test
    void should_return_suggestions_from_valid_json_response() {
        OpenAiHttpDtos.ChatResponse response = new OpenAiHttpDtos.ChatResponse(
                List.of(new OpenAiHttpDtos.Choice(
                        new OpenAiHttpDtos.Message("assistant", "{\"suggestions\": {\"MyController\": \"SavePatientUseCase\"}}"))),
                new OpenAiHttpDtos.Usage(150));
        stubRestClientChain(response);

        OpenAiGpt54AiEnrichmentAdapter adapter = new OpenAiGpt54AiEnrichmentAdapter(
                propertiesWithKey("sk-openai-key"),
                templateLoader,
                restClient,
                objectMapper);

        AiEnrichmentResult result = adapter.call(buildRequest(TaskType.NAMING, "enrichment-naming"));

        assertThat(result.degraded()).isFalse();
        assertThat(result.suggestions()).containsEntry("MyController", "SavePatientUseCase");
        assertThat(result.provider()).isEqualTo(LlmProvider.OPENAI_GPT54);
        assertThat(result.tokensUsed()).isEqualTo(150);
    }

    @Test
    void should_throw_illegal_state_when_api_key_is_absent() {
        OpenAiGpt54AiEnrichmentAdapter adapter = new OpenAiGpt54AiEnrichmentAdapter(
                propertiesWithKey(""),
                templateLoader,
                restClient,
                objectMapper);

        assertThatThrownBy(() -> adapter.call(buildRequest(TaskType.NAMING, "enrichment-naming")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPENAI_API_KEY");
    }

    @Test
    void should_return_degraded_when_response_is_null() {
        stubRestClientChain(null);

        OpenAiGpt54AiEnrichmentAdapter adapter = new OpenAiGpt54AiEnrichmentAdapter(
                propertiesWithKey("sk-openai-key"),
                templateLoader,
                restClient,
                objectMapper);

        AiEnrichmentResult result = adapter.call(buildRequest(TaskType.NAMING, "enrichment-naming"));

        assertThat(result.degraded()).isTrue();
    }

    @Test
    void should_use_configured_max_tokens_for_request_task_type() {
        OpenAiHttpDtos.ChatResponse response = new OpenAiHttpDtos.ChatResponse(
                List.of(new OpenAiHttpDtos.Choice(
                        new OpenAiHttpDtos.Message("assistant", "{\"suggestions\": {\"MyController\": \"GeneratedUseCase\"}}"))),
                new OpenAiHttpDtos.Usage(200));
        stubRestClientChain(response);

        OpenAiGpt54AiEnrichmentAdapter adapter = new OpenAiGpt54AiEnrichmentAdapter(
                propertiesWithKeyAndTokens("sk-openai-key", Map.of(TaskType.ARTIFACT_REVIEW, 2048)),
                templateLoader,
                restClient,
                objectMapper);

        adapter.call(buildRequest(TaskType.ARTIFACT_REVIEW, "artifact-review"));

        org.mockito.ArgumentCaptor<OpenAiHttpDtos.ChatRequest> captor =
                org.mockito.ArgumentCaptor.forClass(OpenAiHttpDtos.ChatRequest.class);
        verify(requestBodyUriSpec).body(captor.capture());
        assertThat(captor.getValue().maxTokens()).isEqualTo(2048);
    }

    @Test
    void should_return_degraded_when_response_exceeds_max_response_size() {
        String oversized = "b".repeat(32);
        OpenAiHttpDtos.ChatResponse response = new OpenAiHttpDtos.ChatResponse(
                List.of(new OpenAiHttpDtos.Choice(
                        new OpenAiHttpDtos.Message("assistant", oversized))),
                new OpenAiHttpDtos.Usage(200));
        stubRestClientChain(response);

        OpenAiGpt54AiEnrichmentAdapter adapter = new OpenAiGpt54AiEnrichmentAdapter(
                propertiesWithKeyAndResponseLimit("sk-openai-key", 16),
                templateLoader,
                restClient,
                objectMapper);

        AiEnrichmentResult result = adapter.call(buildRequest(TaskType.NAMING, "enrichment-naming"));

        assertThat(result.degraded()).isTrue();
        assertThat(result.provider()).isEqualTo(LlmProvider.OPENAI_GPT54);
        assertThat(result.degradationReason()).contains("tronquee").contains("16");
        assertThat(result.suggestions()).isEmpty();
    }
}
