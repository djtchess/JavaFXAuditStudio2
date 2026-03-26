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

        ClaudeHttpDtos.MessagesRequest body = new ClaudeHttpDtos.MessagesRequest(
                DEFAULT_MODEL,
                properties.effectiveMaxTokens(request.taskType()),
                List.of(new ClaudeHttpDtos.Message("user", prompt)));

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
            return AiEnrichmentResult.degraded(request.requestId(), "Reponse Claude vide");
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
        int tokens = response.usage() != null
                ? response.usage().inputTokens() + response.usage().outputTokens()
                : bundle.estimatedTokens();

        LOG.info("Claude — enrichissement nominal, tokens={}, controllerRef={}",
                tokens, bundle.controllerRef());

        return new AiEnrichmentResult(
                request.requestId(), false, "", suggestions, tokens, PROVIDER);
    }

    private String renderPrompt(final AiEnrichmentRequest request) {
        Map<String, Object> context = new java.util.HashMap<>();
        context.put("controllerRef", request.bundle().controllerRef());
        context.put("sanitizedSource", request.bundle().sanitizedSource());
        context.put("estimatedTokens", request.bundle().estimatedTokens());
        context.put("taskType", request.taskType().name());
        context.putAll(request.extraContext());
        return templateLoader.render(request.promptTemplate(), context);
    }

    private String validateAndGetApiKey() {
        String apiKey = properties.activeApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Credential Claude Code absent — configurer CLAUDE_API_KEY");
        }
        return apiKey;
    }
}
