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
 * Adaptateur HTTP vers l'API OpenAI Chat Completions.
 *
 * <p>Envoie le prompt Mustache rendu vers https://api.openai.com/v1/chat/completions.
 * Assemble via {@code AiEnrichmentOrchestraConfiguration} — pas de {@code @Component}.
 */
public class OpenAiGpt54AiEnrichmentAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiGpt54AiEnrichmentAdapter.class);
    private static final LlmProvider PROVIDER = LlmProvider.OPENAI_GPT54;
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o";
    private final AiEnrichmentProperties properties;
    private final PromptTemplateLoader templateLoader;
    private final RestClient restClient;
    private final LlmResponseParser responseParser;

    public OpenAiGpt54AiEnrichmentAdapter(
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

        LOG.debug("OpenAI — envoi requete pour controllerRef={}, taskType={}",
                bundle.controllerRef(), request.taskType());

        OpenAiHttpDtos.ChatRequest body = new OpenAiHttpDtos.ChatRequest(
                DEFAULT_MODEL,
                List.of(new OpenAiHttpDtos.Message("user", prompt)),
                properties.effectiveMaxTokens(request.taskType()));

        OpenAiHttpDtos.ChatResponse response = restClient.post()
                .uri(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OpenAiHttpDtos.ChatResponse.class);

        return buildResult(request, bundle, response);
    }

    private AiEnrichmentResult buildResult(
            final AiEnrichmentRequest request,
            final SanitizedBundle bundle,
            final OpenAiHttpDtos.ChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return AiEnrichmentResult.degraded(request.requestId(), "Reponse OpenAI vide");
        }
        String text = response.choices().get(0).message().content();
        LlmResponseSizeLimiter.LimitedResponse limitedResponse = LlmResponseSizeLimiter.limit(
                text,
                properties.effectiveMaxResponseSizeBytes());
        if (limitedResponse.truncated()) {
            LOG.warn("OpenAI — reponse tronquee car {} > {} octets [requestId={}]",
                    limitedResponse.originalSizeBytes(),
                    properties.effectiveMaxResponseSizeBytes(),
                    request.requestId());
            return AiEnrichmentResult.degraded(
                    request.requestId(),
                    "Reponse OpenAI tronquee car elle depasse "
                            + properties.effectiveMaxResponseSizeBytes() + " octets",
                    PROVIDER);
        }
        Map<String, String> suggestions = responseParser.parse(
                limitedResponse.text(),
                bundle.controllerRef(),
                request.requestId());
        int tokens = response.usage() != null
                ? response.usage().totalTokens()
                : bundle.estimatedTokens();

        LOG.info("OpenAI — enrichissement nominal, tokens={}, controllerRef={}",
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
                    "Credential OpenAI absent — configurer OPENAI_API_KEY");
        }
        return apiKey;
    }
}
