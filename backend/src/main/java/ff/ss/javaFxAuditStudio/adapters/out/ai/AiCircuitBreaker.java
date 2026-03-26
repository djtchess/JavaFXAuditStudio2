package ff.ss.javaFxAuditStudio.adapters.out.ai;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Circuit breaker JDK pur pour l'enrichissement IA (JAS-017).
 *
 * <p>Pas de dependance Resilience4j — comportement identique :
 * <ul>
 *   <li>Fenetre glissante de {@code slidingWindowSize} appels</li>
 *   <li>Ouverture si taux d'echec >= {@code failureRateThreshold} pourcent</li>
 *   <li>Attente de {@code waitDurationInOpenState} avant passage en HALF-OPEN</li>
 *   <li>Retour en CLOSED apres un appel reussi en HALF-OPEN</li>
 * </ul>
 *
 * <p>Thread-safe via {@link ReentrantLock}.
 */
public class AiCircuitBreaker {

    private static final Logger LOG = LoggerFactory.getLogger(AiCircuitBreaker.class);

    /** Etats internes du circuit breaker. */
    enum State { CLOSED, OPEN, HALF_OPEN }

    private final int slidingWindowSize;
    private final int failureRateThreshold;
    private final Duration waitDurationInOpenState;

    private State state = State.CLOSED;
    private final Deque<Boolean> window;
    private long openedAt = 0L;
    private final ReentrantLock lock = new ReentrantLock();

    public AiCircuitBreaker(
            final int slidingWindowSize,
            final int failureRateThreshold,
            final Duration waitDurationInOpenState) {
        this.slidingWindowSize = slidingWindowSize;
        this.failureRateThreshold = failureRateThreshold;
        this.waitDurationInOpenState = waitDurationInOpenState;
        this.window = new ArrayDeque<>(slidingWindowSize);
    }

    /**
     * Execute le fournisseur protege.
     *
     * @param <T>      type du resultat
     * @param supplier fournisseur a appeler
     * @return resultat du fournisseur
     * @throws CircuitOpenException si le circuit est ouvert
     * @throws Exception            si le fournisseur leve une exception
     */
    public <T> T execute(final Supplier<T> supplier) throws Exception {
        lock.lock();
        try {
            transitionIfNeeded();
            if (state == State.OPEN) {
                throw new CircuitOpenException("Circuit ouvert — fournisseur indisponible");
            }
        } finally {
            lock.unlock();
        }

        try {
            T result = supplier.get();
            recordSuccess();
            return result;
        } catch (Exception ex) {
            recordFailure();
            throw ex;
        }
    }

    private void transitionIfNeeded() {
        if (state == State.OPEN) {
            long elapsed = System.currentTimeMillis() - openedAt;
            if (elapsed >= waitDurationInOpenState.toMillis()) {
                LOG.info("Circuit breaker IA passage en HALF_OPEN apres {}ms d'attente", elapsed);
                state = State.HALF_OPEN;
            }
        }
    }

    private void recordSuccess() {
        lock.lock();
        try {
            addToWindow(true);
            if (state == State.HALF_OPEN) {
                LOG.info("Circuit breaker IA retour en CLOSED apres succes en HALF_OPEN");
                state = State.CLOSED;
                window.clear();
            }
        } finally {
            lock.unlock();
        }
    }

    private void recordFailure() {
        lock.lock();
        try {
            addToWindow(false);
            if (state == State.HALF_OPEN || shouldOpen()) {
                LOG.warn("Circuit breaker IA passage en OPEN — taux d'echec depasse le seuil");
                state = State.OPEN;
                openedAt = System.currentTimeMillis();
                window.clear();
            }
        } finally {
            lock.unlock();
        }
    }

    private void addToWindow(final boolean success) {
        if (window.size() >= slidingWindowSize) {
            window.pollFirst();
        }
        window.addLast(success);
    }

    private boolean shouldOpen() {
        if (window.size() < slidingWindowSize) {
            return false;
        }
        long failures = window.stream().filter(b -> !b).count();
        int rate = (int) ((failures * 100) / window.size());
        return rate >= failureRateThreshold;
    }

    /** Exception levee quand le circuit est en etat OPEN. */
    static class CircuitOpenException extends RuntimeException {
        CircuitOpenException(final String message) {
            super(message);
        }
    }
}
