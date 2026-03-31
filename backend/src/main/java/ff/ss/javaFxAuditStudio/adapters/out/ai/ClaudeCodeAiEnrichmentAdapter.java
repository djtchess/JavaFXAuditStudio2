package ff.ss.javaFxAuditStudio.adapters.out.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Adaptateur HTTP vers l'API Anthropic Claude Messages.
 *
 * <p>Envoie le prompt Mustache rendu vers https://api.anthropic.com/v1/messages.
 * Parse la reponse JSON pour extraire les suggestions.
 * Assemble via {@code AiEnrichmentOrchestraConfiguration} — pas de {@code @Component}.
 */
public class ClaudeCodeAiEnrichmentAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ClaudeCodeAiEnrichmentAdapter.class);
    private static final LlmProvider PROVIDER = LlmProvider.CLAUDE_CODE;
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-6";
    private static final double DEFAULT_TEMPERATURE = 0.0d;
    private final AiEnrichmentProperties properties;
    private final PromptTemplateLoader templateLoader;
    private final RestClient restClient;
    private final LlmResponseParser responseParser;

    public ClaudeCodeAiEnrichmentAdapter(
            final AiEnrichmentProperties properties,
            final PromptTemplateLoader templateLoader,
            final RestClient restClient,
            final ObjectMapper objectMapper) {
        this.properties = Objects.requireNonNull(properties);
        this.templateLoader = Objects.requireNonNull(templateLoader);
        this.restClient = Objects.requireNonNull(restClient);
        this.responseParser = new LlmResponseParser(objectMapper);
    }

    public AiEnrichmentResult call(final AiEnrichmentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String apiKey = validateAndGetApiKey();

        SanitizedBundle bundle = request.bundle();
        String prompt = renderPrompt(request);

        LOG.debug("Claude — envoi requete pour controllerRef={}, taskType={}",
                bundle.controllerRef(), request.taskType());

        String systemPrompt = buildSystemPrompt(request);
        ClaudeHttpDtos.MessagesRequest body = new ClaudeHttpDtos.MessagesRequest(
                DEFAULT_MODEL,
                properties.effectiveMaxTokens(request.taskType()),
                systemPrompt,
                List.of(new ClaudeHttpDtos.Message("user", prompt)),
                DEFAULT_TEMPERATURE);

        ClaudeHttpDtos.MessagesResponse response = restClient.post()
                .uri(API_URL)
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ClaudeHttpDtos.MessagesResponse.class);

        return buildResult(request, bundle, response);
    }

    private AiEnrichmentResult buildResult(
            final AiEnrichmentRequest request,
            final SanitizedBundle bundle,
            final ClaudeHttpDtos.MessagesResponse response) {
        if (response == null || response.content() == null || response.content().isEmpty()) {
            return AiEnrichmentResult.degraded(request.requestId(), "Reponse Claude vide", PROVIDER);
        }
        String text = response.content().stream()
                .filter(b -> "text".equals(b.type()))
                .map(ClaudeHttpDtos.ContentBlock::text)
                .findFirst()
                .orElse("");
        LlmResponseSizeLimiter.LimitedResponse limitedResponse = LlmResponseSizeLimiter.limit(
                text,
                properties.effectiveMaxResponseSizeBytes());
        if (limitedResponse.truncated()) {
            LOG.warn("Claude — reponse tronquee car {} > {} octets [requestId={}]",
                    limitedResponse.originalSizeBytes(),
                    properties.effectiveMaxResponseSizeBytes(),
                    request.requestId());
            return AiEnrichmentResult.degraded(
                    request.requestId(),
                    "Reponse Claude tronquee car elle depasse "
                            + properties.effectiveMaxResponseSizeBytes() + " octets",
                    PROVIDER);
        }

        Map<String, String> suggestions = responseParser.parse(
                limitedResponse.text(),
                bundle.controllerRef(),
                request.requestId());
        if (suggestions.isEmpty()) {
            LOG.warn("Claude - reponse non structuree ou vide [requestId={}, controllerRef={}]",
                    request.requestId(),
                    bundle.controllerRef());
            return AiEnrichmentResult.degraded(
                    request.requestId(),
                    "Reponse Claude non structuree ou vide",
                    PROVIDER);
        }
        int tokens = response.usage() != null
                ? response.usage().inputTokens() + response.usage().outputTokens()
                : bundle.estimatedTokens();

        LOG.info("Claude — enrichissement nominal, tokens={}, controllerRef={}",
                tokens, bundle.controllerRef());

        return new AiEnrichmentResult(
                request.requestId(), false, "", suggestions, tokens, PROVIDER);
    }

    private String renderPrompt(final AiEnrichmentRequest request) {
        Map<String, Object> context = PromptContextBudgetSupport.budgetContext(properties, request);
        return templateLoader.render(request.promptTemplate(), context);
    }

    private String buildSystemPrompt(final AiEnrichmentRequest request) {
        return StructuredOutputContract.strictSystemPrompt(
                request.bundle().controllerRef(),
                request.taskType());
    }

    private String validateAndGetApiKey() {
        String apiKey = properties.apiKeyFor(PROVIDER);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Credential Claude Code absent — configurer CLAUDE_API_KEY");
        }
        return apiKey;
    }
}
