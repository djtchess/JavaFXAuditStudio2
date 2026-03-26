package ff.ss.javaFxAuditStudio.adapters.out.ai;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;

/**
 * Adaptateur de routage vers le fournisseur IA configuré (IAP-2).
 *
 * <p>Délègue vers {@link ClaudeCodeAiEnrichmentAdapter}, {@link OpenAiGpt54AiEnrichmentAdapter}
 * ou {@link ClaudeCodeCliAiEnrichmentAdapter} selon la valeur de {@code ai.enrichment.provider}.
 * Le switch sur {@link LlmProvider} remplace les comparaisons de String.
 *
 * <p>Assemble via {@code AiEnrichmentOrchestraConfiguration} — pas de {@code @Component}.
 */
public class RoutingAiEnrichmentAdapter implements AiEnrichmentPort {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingAiEnrichmentAdapter.class);
    private static final String REASON_DISABLED = "AI enrichment disabled";
    private static final String REASON_CIRCUIT_OPEN = "Circuit ouvert — fournisseur indisponible";
    private static final String REASON_UNKNOWN = "Erreur inconnue";

    private final AiEnrichmentProperties properties;
    private final ClaudeCodeAiEnrichmentAdapter claudeAdapter;
    private final OpenAiGpt54AiEnrichmentAdapter openAiAdapter;
    private final ClaudeCodeCliAiEnrichmentAdapter cliAdapter;
    private final AiCircuitBreaker circuitBreaker;

    public RoutingAiEnrichmentAdapter(
            final AiEnrichmentProperties properties,
            final ClaudeCodeAiEnrichmentAdapter claudeAdapter,
            final OpenAiGpt54AiEnrichmentAdapter openAiAdapter,
            final ClaudeCodeCliAiEnrichmentAdapter cliAdapter,
            final AiCircuitBreaker circuitBreaker) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.claudeAdapter = Objects.requireNonNull(claudeAdapter, "claudeAdapter must not be null");
        this.openAiAdapter = Objects.requireNonNull(openAiAdapter, "openAiAdapter must not be null");
        this.cliAdapter = Objects.requireNonNull(cliAdapter, "cliAdapter must not be null");
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker must not be null");
    }

    @Override
    public AiEnrichmentResult enrich(final AiEnrichmentRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        if (!properties.enabled()) {
            return AiEnrichmentResult.degraded(request.requestId(), REASON_DISABLED);
        }

        LOG.debug("Enrichissement — tokens estimes : {}", request.bundle().estimatedTokens());

        return callWithCircuitBreaker(request);
    }

    private AiEnrichmentResult callWithCircuitBreaker(final AiEnrichmentRequest request) {
        LlmProvider provider = properties.providerEnum();
        AiEnrichmentProperties.Retry retryConfig = properties.effectiveRetry();
        LlmRetryPolicy retryPolicy = new LlmRetryPolicy(
                retryConfig.effectiveMaxRetries(),
                retryConfig.effectiveInitialBackoffMs(),
                retryConfig.effectiveMultiplier());
        try {
            return circuitBreaker.execute(() -> {
                try {
                    return retryPolicy.execute(() -> route(request, provider));
                } catch (RuntimeException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (AiCircuitBreaker.CircuitOpenException ex) {
            LOG.warn("Circuit breaker ouvert pour le fournisseur {}", provider);
            return AiEnrichmentResult.degraded(request.requestId(), REASON_CIRCUIT_OPEN, provider);
        } catch (Exception ex) {
            String reason = truncate(ex.getMessage());
            LOG.warn("Erreur fournisseur {} — mode degrade actif", provider);
            return AiEnrichmentResult.degraded(request.requestId(), reason, provider);
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
}
