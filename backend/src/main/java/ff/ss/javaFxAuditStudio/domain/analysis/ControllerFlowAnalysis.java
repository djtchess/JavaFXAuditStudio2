package ff.ss.javaFxAuditStudio.domain.analysis;

import java.util.List;
import java.util.Objects;

public record ControllerFlowAnalysis(
        String controllerRef,
        String controllerName,
        boolean stateMachineDetected,
        double stateMachineConfidence,
        String detectionLevel,
        List<String> states,
        List<StateTransition> transitions,
        List<String> policyGuardCandidates,
        List<String> uiGuardMethods,
        List<String> evidence,
        List<String> warnings) {

    public ControllerFlowAnalysis {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(controllerName, "controllerName must not be null");
        Objects.requireNonNull(detectionLevel, "detectionLevel must not be null");
        Objects.requireNonNull(states, "states must not be null");
        Objects.requireNonNull(transitions, "transitions must not be null");
        Objects.requireNonNull(policyGuardCandidates, "policyGuardCandidates must not be null");
        Objects.requireNonNull(uiGuardMethods, "uiGuardMethods must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");
        Objects.requireNonNull(warnings, "warnings must not be null");
        if (stateMachineConfidence < 0.0d || stateMachineConfidence > 1.0d) {
            throw new IllegalArgumentException("stateMachineConfidence must be between 0 and 1");
        }
        states = List.copyOf(states);
        transitions = List.copyOf(transitions);
        policyGuardCandidates = List.copyOf(policyGuardCandidates);
        uiGuardMethods = List.copyOf(uiGuardMethods);
        evidence = List.copyOf(evidence);
        warnings = List.copyOf(warnings);
    }

    public record StateTransition(
            String sourceState,
            String targetState,
            String triggerMethod,
            String guardExpression,
            int sourceLine) {

        public StateTransition {
            Objects.requireNonNull(sourceState, "sourceState must not be null");
            Objects.requireNonNull(targetState, "targetState must not be null");
            Objects.requireNonNull(triggerMethod, "triggerMethod must not be null");
            Objects.requireNonNull(guardExpression, "guardExpression must not be null");
            if (sourceLine < 0) {
                throw new IllegalArgumentException("sourceLine must be >= 0");
            }
        }
    }
}
