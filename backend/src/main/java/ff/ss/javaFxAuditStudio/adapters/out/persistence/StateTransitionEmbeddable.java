package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class StateTransitionEmbeddable {

    @Column(name = "from_state", nullable = false, length = 120)
    private String fromState;

    @Column(name = "to_state", nullable = false, length = 120)
    private String toState;

    @Column(name = "trigger_name", nullable = false, length = 255)
    private String trigger;

    protected StateTransitionEmbeddable() {
    }

    public StateTransitionEmbeddable(
            final String fromState,
            final String toState,
            final String trigger) {
        this.fromState = fromState;
        this.toState = toState;
        this.trigger = trigger;
    }

    public String getFromState() {
        return fromState;
    }

    public String getToState() {
        return toState;
    }

    public String getTrigger() {
        return trigger;
    }
}
