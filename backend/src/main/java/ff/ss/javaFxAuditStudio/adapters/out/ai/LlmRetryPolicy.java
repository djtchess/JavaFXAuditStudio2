package ff.ss.javaFxAuditStudio.adapters.out.ai;

import java.util.function.LongConsumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Politique de retry avec backoff exponentiel pour les appels LLM (IAP-4).
 *
 * <p>Logique :
 * - Les erreurs HTTP 429 et 503 declenchent un retry.
 * - Les erreurs HTTP 400 (et tout autre 4xx) ne declenchent pas de retry.
 * - Les {@link ResourceAccessException} (timeout reseau) declenchent un retry.
 * - Le delai double a chaque tentative : initialBackoffMs * multiplier^attempt.
 *
 * <p>Classe package-private : uniquement utilisee par {@link RoutingAiEnrichmentAdapter}.
 */
class LlmRetryPolicy {

    private static final Logger LOG = LoggerFactory.getLogger(LlmRetryPolicy.class);

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVICE_UNAVAILABLE = 503;

    private final int maxRetries;
    private final long initialBackoffMs;
    private final double multiplier;
    private final LongConsumer sleeper;

    LlmRetryPolicy(
            final int maxRetries,
            final long initialBackoffMs,
            final double multiplier,
            final LongConsumer sleeper) {
        this.maxRetries = maxRetries;
        this.initialBackoffMs = initialBackoffMs;
        this.multiplier = multiplier;
        this.sleeper = sleeper;
    }

    LlmRetryPolicy(final int maxRetries, final long initialBackoffMs, final double multiplier) {
        this(maxRetries, initialBackoffMs, multiplier, ms -> {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    <T> T execute(final Supplier<T> supplier) throws Exception {
        int attempt = 0;
        while (true) {
            try {
                return supplier.get();
            } catch (Exception ex) {
                if (!isRetryable(ex) || attempt >= maxRetries) {
                    throw ex;
                }
                long delay = (long) (initialBackoffMs * Math.pow(multiplier, attempt));
                LOG.warn(
                        "Retry tentative {}/{} apres {}ms — raison : {}",
                        attempt + 1,
                        maxRetries,
                        delay,
                        extractReason(ex));
                sleeper.accept(delay);
                attempt++;
            }
        }
    }

    private boolean isRetryable(final Exception ex) {
        if (ex instanceof ResourceAccessException) {
            return true;
        }
        if (ex instanceof HttpStatusCodeException httpEx) {
            int statusCode = httpEx.getStatusCode().value();
            return statusCode == HTTP_TOO_MANY_REQUESTS || statusCode == HTTP_SERVICE_UNAVAILABLE;
        }
        return false;
    }

    private String extractReason(final Exception ex) {
        String message = ex.getMessage();
        return (message != null && !message.isBlank()) ? message : ex.getClass().getSimpleName();
    }
}
