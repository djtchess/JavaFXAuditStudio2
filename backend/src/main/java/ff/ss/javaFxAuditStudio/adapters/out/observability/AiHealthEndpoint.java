package ff.ss.javaFxAuditStudio.adapters.out.observability;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import ff.ss.javaFxAuditStudio.adapters.out.ai.AiCircuitBreaker;
import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties;

/**
 * Endpoint Actuator agregeant les KPI techniques des appels IA.
 */
@Endpoint(id = "aihealth")
public final class AiHealthEndpoint {

    private static final String REQUEST_TOTAL_METRIC = "llm.requests.total";
    private static final String REQUEST_DURATION_METRIC = "llm.requests.duration";
    private static final String TOKENS_USED_METRIC = "llm.tokens.used";
    private static final String GLOBAL_TAG_VALUE = "all";

    private final MeterRegistry meterRegistry;
    private final AiEnrichmentProperties properties;
    private final AiCircuitBreaker circuitBreaker;

    public AiHealthEndpoint(
            final MeterRegistry meterRegistry,
            final AiEnrichmentProperties properties,
            final AiCircuitBreaker circuitBreaker) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker must not be null");
    }

    @ReadOperation
    public AiHealthResponse aiHealth() {
        Map<String, Long> outcomes = collectOutcomes();
        long totalRequests = resolveTotalRequests(outcomes);
        long successfulRequests = outcomes.getOrDefault("success", 0L);
        double successRate = computeSuccessRate(totalRequests, successfulRequests);
        double p95LatencyMs = resolveP95LatencyMs();
        double totalTokens = resolveTotalTokens();
        String circuitState = circuitBreaker.currentState();

        return new AiHealthResponse(
                resolveStatus(totalRequests, successRate, circuitState),
                properties.enabled(),
                properties.provider(),
                circuitState,
                totalRequests,
                roundToOneDecimal(successRate),
                roundToOneDecimal(p95LatencyMs),
                roundToOneDecimal(totalTokens),
                outcomes);
    }

    private Map<String, Long> collectOutcomes() {
        Map<String, Long> outcomes = new LinkedHashMap<>();

        for (Counter counter : meterRegistry.find(REQUEST_TOTAL_METRIC).counters()) {
            String status = counter.getId().getTag("status");
            if (status != null && !status.isBlank()) {
                outcomes.merge(status, Math.round(counter.count()), Long::sum);
            }
        }
        return Map.copyOf(outcomes);
    }

    private long resolveTotalRequests(final Map<String, Long> outcomes) {
        Counter globalCounter = findGlobalCounter(REQUEST_TOTAL_METRIC);
        long totalRequests = outcomes.values().stream().mapToLong(Long::longValue).sum();

        if (globalCounter != null) {
            totalRequests = Math.round(globalCounter.count());
        }
        return totalRequests;
    }

    private double computeSuccessRate(
            final long totalRequests,
            final long successfulRequests) {
        double successRate = 0.0d;

        if (totalRequests > 0L) {
            successRate = (successfulRequests * 100.0d) / totalRequests;
        }
        return successRate;
    }

    private double resolveP95LatencyMs() {
        Timer globalTimer = findGlobalTimer(REQUEST_DURATION_METRIC);
        double p95LatencyMs = 0.0d;

        if (globalTimer != null) {
            p95LatencyMs = fallbackLatencyMs(globalTimer);
            for (ValueAtPercentile percentile : globalTimer.takeSnapshot().percentileValues()) {
                if (Math.abs(percentile.percentile() - 0.95d) < 0.001d) {
                    p95LatencyMs = percentile.value(TimeUnit.MILLISECONDS);
                }
            }
        }
        return p95LatencyMs;
    }

    private double fallbackLatencyMs(final Timer globalTimer) {
        double latencyMs = 0.0d;

        if (globalTimer.count() > 0L) {
            latencyMs = globalTimer.mean(TimeUnit.MILLISECONDS);
        }
        return latencyMs;
    }

    private double resolveTotalTokens() {
        DistributionSummary globalSummary = findGlobalSummary(TOKENS_USED_METRIC);
        double totalTokens = 0.0d;

        if (globalSummary != null) {
            totalTokens = globalSummary.totalAmount();
        } else {
            totalTokens = meterRegistry.find(TOKENS_USED_METRIC).summaries().stream()
                    .filter(summary -> !GLOBAL_TAG_VALUE.equals(summary.getId().getTag("provider")))
                    .mapToDouble(DistributionSummary::totalAmount)
                    .sum();
        }
        return totalTokens;
    }

    private Counter findGlobalCounter(final String meterName) {
        Counter counter = null;

        for (Meter meter : meterRegistry.find(meterName).meters()) {
            if (GLOBAL_TAG_VALUE.equals(meter.getId().getTag("provider"))
                    && GLOBAL_TAG_VALUE.equals(meter.getId().getTag("taskType"))
                    && GLOBAL_TAG_VALUE.equals(meter.getId().getTag("status"))
                    && meter instanceof Counter currentCounter) {
                counter = currentCounter;
            }
        }
        return counter;
    }

    private Timer findGlobalTimer(final String meterName) {
        Timer timer = null;

        for (Meter meter : meterRegistry.find(meterName).meters()) {
            if (GLOBAL_TAG_VALUE.equals(meter.getId().getTag("provider"))
                    && GLOBAL_TAG_VALUE.equals(meter.getId().getTag("taskType"))
                    && GLOBAL_TAG_VALUE.equals(meter.getId().getTag("status"))
                    && meter instanceof Timer currentTimer) {
                timer = currentTimer;
            }
        }
        return timer;
    }

    private DistributionSummary findGlobalSummary(final String meterName) {
        DistributionSummary summary = null;

        for (Meter meter : meterRegistry.find(meterName).meters()) {
            if (GLOBAL_TAG_VALUE.equals(meter.getId().getTag("provider"))
                    && GLOBAL_TAG_VALUE.equals(meter.getId().getTag("taskType"))
                    && meter instanceof DistributionSummary currentSummary) {
                summary = currentSummary;
            }
        }
        return summary;
    }

    private String resolveStatus(
            final long totalRequests,
            final double successRate,
            final String circuitState) {
        String status = "IDLE";

        if (!properties.enabled()) {
            status = "DISABLED";
        } else if ("OPEN".equalsIgnoreCase(circuitState)) {
            status = "DEGRADED";
        } else if (totalRequests > 0L && successRate >= 80.0d) {
            status = "UP";
        } else if (totalRequests > 0L) {
            status = "DEGRADED";
        }
        return status;
    }

    private double roundToOneDecimal(final double value) {
        return Math.round(value * 10.0d) / 10.0d;
    }

    public record AiHealthResponse(
            String status,
            boolean enabled,
            String provider,
            String circuitBreakerState,
            long totalRequests,
            double successRate,
            double p95LatencyMs,
            double totalTokens,
            Map<String, Long> outcomes) {
    }
}
