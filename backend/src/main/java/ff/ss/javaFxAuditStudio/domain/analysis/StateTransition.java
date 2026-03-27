package ff.ss.javaFxAuditStudio.domain.analysis;

import java.util.Objects;

/**
 * Transition de logique d'etat detectee dans un controller.
 */
public record StateTransition(
        String fromState,
        String toState,
        String trigger) {

    public StateTransition {
        Objects.requireNonNull(fromState, "fromState must not be null");
        Objects.requireNonNull(toState, "toState must not be null");
        Objects.requireNonNull(trigger, "trigger must not be null");
    }
}
