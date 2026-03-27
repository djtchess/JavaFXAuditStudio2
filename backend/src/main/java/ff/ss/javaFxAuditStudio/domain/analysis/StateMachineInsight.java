package ff.ss.javaFxAuditStudio.domain.analysis;

import java.util.List;
import java.util.Objects;

/**
 * Synthese d'une logique d'etat detectee dans un controller.
 */
public record StateMachineInsight(
        DetectionStatus status,
        double confidence,
        List<String> states,
        List<StateTransition> transitions) {

    public StateMachineInsight {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(states, "states must not be null");
        Objects.requireNonNull(transitions, "transitions must not be null");
        if (confidence < 0.0d || confidence > 1.0d) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
        states = List.copyOf(states);
        transitions = List.copyOf(transitions);
    }

    public static StateMachineInsight absent() {
        return new StateMachineInsight(DetectionStatus.ABSENT, 0.0d, List.of(), List.of());
    }
}
