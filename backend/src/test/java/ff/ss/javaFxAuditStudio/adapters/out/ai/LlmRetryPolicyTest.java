package ff.ss.javaFxAuditStudio.adapters.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Tests unitaires de {@link LlmRetryPolicy} (IAP-4).
 *
 * <p>Le sleeper est remplace par un no-op pour eviter tout {@link Thread#sleep} reel.
 */
class LlmRetryPolicyTest {

    private static final int MAX_RETRIES = 2;
    private static final long INITIAL_BACKOFF_MS = 500L;
    private static final double MULTIPLIER = 2.0;

    /** Construit la politique avec un sleeper no-op (les tests ne dorment pas). */
    private LlmRetryPolicy buildPolicy() {
        return new LlmRetryPolicy(MAX_RETRIES, INITIAL_BACKOFF_MS, MULTIPLIER, ms -> {
            // no-op intentionnel — pas de sleep reel dans les tests
        });
    }

    @Test
    void should_succeed_on_first_attempt_without_retry() throws Exception {
        LlmRetryPolicy policy = buildPolicy();

        String result = policy.execute(() -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void should_retry_on_429_and_succeed() throws Exception {
        LlmRetryPolicy policy = buildPolicy();
        AtomicInteger callCount = new AtomicInteger(0);

        String result = policy.execute(() -> {
            if (callCount.getAndIncrement() == 0) {
                throw HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, null, null);
            }
            return "retried-ok";
        });

        assertThat(result).isEqualTo("retried-ok");
        assertThat(callCount.get()).isEqualTo(2);
    }

    @Test
    void should_retry_on_503_and_succeed() throws Exception {
        LlmRetryPolicy policy = buildPolicy();
        AtomicInteger callCount = new AtomicInteger(0);

        String result = policy.execute(() -> {
            if (callCount.getAndIncrement() == 0) {
                throw HttpServerErrorException.create(
                        HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", null, null, null);
            }
            return "retried-ok";
        });

        assertThat(result).isEqualTo("retried-ok");
        assertThat(callCount.get()).isEqualTo(2);
    }

    @Test
    void should_not_retry_on_400() {
        LlmRetryPolicy policy = buildPolicy();
        AtomicInteger callCount = new AtomicInteger(0);

        HttpClientErrorException ex400 = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", null, null, null);

        assertThatThrownBy(() -> policy.execute(() -> {
            callCount.incrementAndGet();
            throw ex400;
        })).isSameAs(ex400);

        assertThat(callCount.get())
                .as("Aucun retry ne doit etre tente pour une erreur 400")
                .isEqualTo(1);
    }

    @Test
    void should_throw_after_max_retries_exhausted() {
        LlmRetryPolicy policy = buildPolicy();
        AtomicInteger callCount = new AtomicInteger(0);

        HttpServerErrorException ex503 = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", null, null, null);

        assertThatThrownBy(() -> policy.execute(() -> {
            callCount.incrementAndGet();
            throw ex503;
        })).isSameAs(ex503);

        // 1 appel initial + MAX_RETRIES tentatives supplementaires
        assertThat(callCount.get())
                .as("Le nombre total d'appels doit etre maxRetries + 1")
                .isEqualTo(MAX_RETRIES + 1);
    }

    @Test
    void should_retry_on_resource_access_exception() throws Exception {
        LlmRetryPolicy policy = buildPolicy();
        AtomicInteger callCount = new AtomicInteger(0);

        String result = policy.execute(() -> {
            if (callCount.getAndIncrement() == 0) {
                throw new ResourceAccessException("Connection refused");
            }
            return "network-retry-ok";
        });

        assertThat(result).isEqualTo("network-retry-ok");
        assertThat(callCount.get()).isEqualTo(2);
    }

    @Test
    void should_apply_exponential_backoff() throws Exception {
        List<Long> capturedDelays = new ArrayList<>();
        LlmRetryPolicy policy = new LlmRetryPolicy(
                MAX_RETRIES,
                INITIAL_BACKOFF_MS,
                MULTIPLIER,
                capturedDelays::add);

        AtomicInteger callCount = new AtomicInteger(0);

        policy.execute(() -> {
            int attempt = callCount.getAndIncrement();
            if (attempt < MAX_RETRIES) {
                throw HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, null, null);
            }
            return "done";
        });

        // Deux retries => deux delais : 500ms et 1000ms
        assertThat(capturedDelays).hasSize(MAX_RETRIES);
        assertThat(capturedDelays.get(0))
                .as("Premier delai : initialBackoffMs * multiplier^0 = 500ms")
                .isEqualTo(500L);
        assertThat(capturedDelays.get(1))
                .as("Deuxieme delai : initialBackoffMs * multiplier^1 = 1000ms")
                .isEqualTo(1000L);
    }
}
