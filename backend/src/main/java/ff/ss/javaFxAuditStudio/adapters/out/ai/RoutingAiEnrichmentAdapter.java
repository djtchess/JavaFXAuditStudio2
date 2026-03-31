package ff.ss.javaFxAuditStudio.adapters.out.ai;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;

/**
 * Adaptateur de routage vers le fournisseur IA configure (IAP-2).
 *
 * <p>Delegue vers {@link ClaudeCodeAiEnrichmentAdapter}, {@link OpenAiGpt54AiEnrichmentAdapter}
 * ou {@link ClaudeCodeCliAiEnrichmentAdapter} selon la valeur de {@code ai.enrichment.provider}.
 * Le switch sur {@link LlmProvider} remplace les comparaisons de String.
 *
 * <p>Assemble via {@code AiEnrichmentOrchestraConfiguration} - pas de {@code @Component}.
 */
public class RoutingAiEnrichmentAdapter implements AiEnrichmentPort {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingAiEnrichmentAdapter.class);
    private static final String REASON_DISABLED = "AI enrichment disabled";
    private static final String REASON_CIRCUIT_OPEN = "Circuit ouvert - fournisseur indisponible";
    private static final String REASON_UNKNOWN = "Erreur inconnue";
    private static final String REQUEST_TOTAL_METRIC = "llm.requests.total";
    private static final String REQUEST_DURATION_METRIC = "llm.requests.duration";
    private static final String TOKENS_USED_METRIC = "llm.tokens.used";
    private static final String GLOBAL_TAG_VALUE = "all";

    private final AiEnrichmentProperties properties;
    private final ClaudeCodeAiEnrichmentAdapter claudeAdapter;
    private final OpenAiGpt54AiEnrichmentAdapter openAiAdapter;
    private final ClaudeCodeCliAiEnrichmentAdapter cliAdapter;
    private final AiCircuitBreaker circuitBreaker;
    private final MeterRegistry meterRegistry;

    public RoutingAiEnrichmentAdapter(
            final AiEnrichmentProperties properties,
            final ClaudeCodeAiEnrichmentAdapter claudeAdapter,
            final OpenAiGpt54AiEnrichmentAdapter openAiAdapter,
            final ClaudeCodeCliAiEnrichmentAdapter cliAdapter,
            final AiCircuitBreaker circuitBreaker,
            final MeterRegistry meterRegistry) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.claudeAdapter = Objects.requireNonNull(claudeAdapter, "claudeAdapter must not be null");
        this.openAiAdapter = Objects.requireNonNull(openAiAdapter, "openAiAdapter must not be null");
        this.cliAdapter = Objects.requireNonNull(cliAdapter, "cliAdapter must not be null");
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    @Override
    public AiEnrichmentResult enrich(final AiEnrichmentRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        if (!properties.enabled()) {
            return AiEnrichmentResult.degraded(request.requestId(), REASON_DISABLED);
        }

        LOG.debug("Enrichissement - tokens estimes : {}", request.bundle().estimatedTokens());

        return callWithCircuitBreaker(request);
    }

    private AiEnrichmentResult callWithCircuitBreaker(final AiEnrichmentRequest request) {
        LlmProvider provider = properties.providerEnum();
        AiEnrichmentProperties.Retry retryConfig = properties.effectiveRetry();
        LlmRetryPolicy retryPolicy = new LlmRetryPolicy(
                retryConfig.effectiveMaxRetries(),
                retryConfig.effectiveInitialBackoffMs(),
                retryConfig.effectiveMultiplier());
        Instant startedAt = Instant.now();

        try {
            AiEnrichmentResult result = circuitBreaker.execute(() -> executeRequest(request, provider, retryPolicy));
            recordRequestMetrics(
                    provider.value(),
                    request.taskType().name(),
                    resolveOutcome(result),
                    Duration.between(startedAt, Instant.now()),
                    result.tokensUsed());
            return result;
        } catch (AiCircuitBreaker.CircuitOpenException ex) {
            LOG.warn("Circuit breaker ouvert pour le fournisseur {}", provider);
            recordRequestMetrics(
                    provider.value(),
                    request.taskType().name(),
                    "circuit_open",
                    Duration.between(startedAt, Instant.now()),
                    0);
            return AiEnrichmentResult.degraded(request.requestId(), REASON_CIRCUIT_OPEN, provider);
        } catch (Exception ex) {
            String reason = truncate(ex.getMessage());
            LOG.warn("Erreur fournisseur {} - mode degrade actif", provider);
            recordRequestMetrics(
                    provider.value(),
                    request.taskType().name(),
                    "failure",
                    Duration.between(startedAt, Instant.now()),
                    0);
            return AiEnrichmentResult.degraded(request.requestId(), reason, provider);
        }
    }

    private AiEnrichmentResult executeRequest(
            final AiEnrichmentRequest request,
            final LlmProvider provider,
            final LlmRetryPolicy retryPolicy) {
        try {
            return retryPolicy.execute(() -> route(request, provider));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private AiEnrichmentResult route(final AiEnrichmentRequest request, final LlmProvider provider) {
        return switch (provider) {
            case CLAUDE_CODE -> claudeAdapter.call(request);
            case CLAUDE_CODE_CLI -> cliAdapter.call(request);
            default -> openAiAdapter.call(request);
        };
    }

    private String truncate(final String message) {
        String safeMessage = message;
        String truncatedMessage;
        int maxReasonLength = properties.effectiveMaxDegradationReasonLength();

        if (safeMessage == null) {
            safeMessage = REASON_UNKNOWN;
        }
        truncatedMessage = safeMessage;
        if (safeMessage.length() > maxReasonLength) {
            truncatedMessage = safeMessage.substring(0, maxReasonLength) + "...";
        }
        return truncatedMessage;
    }

    private String resolveOutcome(final AiEnrichmentResult result) {
        String outcome;

        outcome = "success";
        if (result.degraded()) {
            outcome = "degraded";
        }
        return outcome;
    }

    private void recordRequestMetrics(
            final String provider,
            final String taskType,
            final String status,
            final Duration duration,
            final int tokensUsed) {
        Duration safeDuration = (duration != null) ? duration : Duration.ZERO;
        String providerTag = normalizeTag(provider);
        String taskTypeTag = normalizeTag(taskType);
        String statusTag = normalizeTag(status);

        Timer.builder(REQUEST_DURATION_METRIC)
                .tags("provider", providerTag, "taskType", taskTypeTag, "status", statusTag)
                .register(meterRegistry)
                .record(safeDuration);
        Timer.builder(REQUEST_DURATION_METRIC)
                .tags("provider", GLOBAL_TAG_VALUE, "taskType", GLOBAL_TAG_VALUE, "status", GLOBAL_TAG_VALUE)
                .register(meterRegistry)
                .record(safeDuration);
        Counter.builder(REQUEST_TOTAL_METRIC)
                .tags("provider", providerTag, "taskType", taskTypeTag, "status", statusTag)
                .register(meterRegistry)
                .increment();
        Counter.builder(REQUEST_TOTAL_METRIC)
                .tags("provider", GLOBAL_TAG_VALUE, "taskType", GLOBAL_TAG_VALUE, "status", GLOBAL_TAG_VALUE)
                .register(meterRegistry)
                .increment();
        recordTokenUsage(providerTag, taskTypeTag, tokensUsed);
    }

    private void recordTokenUsage(
            final String provider,
            final String taskType,
            final int tokensUsed) {
        if (tokensUsed > 0) {
            DistributionSummary.builder(TOKENS_USED_METRIC)
                    .baseUnit("tokens")
                    .tags("provider", provider, "taskType", taskType)
                    .register(meterRegistry)
                    .record(tokensUsed);
            DistributionSummary.builder(TOKENS_USED_METRIC)
                    .baseUnit("tokens")
                    .tags("provider", GLOBAL_TAG_VALUE, "taskType", GLOBAL_TAG_VALUE)
                    .register(meterRegistry)
                    .record(tokensUsed);
        }
    }

    private String normalizeTag(final String value) {
        String normalized;

        normalized = "unknown";
        if (value != null && !value.isBlank()) {
            normalized = value.trim().toLowerCase();
        }
        return normalized;
    }
}
