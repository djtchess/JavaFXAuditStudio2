package ff.ss.javaFxAuditStudio.domain.analysis;

import java.util.List;
import java.util.Objects;

public record ProjectDeltaAnalysis(
        String projectId,
        String baselineLabel,
        String currentLabel,
        int newControllers,
        int removedControllers,
        int modifiedControllers,
        int unchangedControllers,
        List<ControllerDelta> controllerDeltas,
        List<String> warnings) {

    public ProjectDeltaAnalysis {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(baselineLabel, "baselineLabel must not be null");
        Objects.requireNonNull(currentLabel, "currentLabel must not be null");
        Objects.requireNonNull(controllerDeltas, "controllerDeltas must not be null");
        Objects.requireNonNull(warnings, "warnings must not be null");
        if (newControllers < 0 || removedControllers < 0 || modifiedControllers < 0 || unchangedControllers < 0) {
            throw new IllegalArgumentException("controller counts must be >= 0");
        }
        controllerDeltas = List.copyOf(controllerDeltas);
        warnings = List.copyOf(warnings);
    }

    public record ControllerDelta(
            String controllerRef,
            DeltaStatus status,
            List<String> addedRules,
            List<String> removedRules,
            List<String> addedTransitions,
            List<String> removedTransitions,
            List<String> notes) {

        public ControllerDelta {
            Objects.requireNonNull(controllerRef, "controllerRef must not be null");
            Objects.requireNonNull(status, "status must not be null");
            Objects.requireNonNull(addedRules, "addedRules must not be null");
            Objects.requireNonNull(removedRules, "removedRules must not be null");
            Objects.requireNonNull(addedTransitions, "addedTransitions must not be null");
            Objects.requireNonNull(removedTransitions, "removedTransitions must not be null");
            Objects.requireNonNull(notes, "notes must not be null");
            addedRules = List.copyOf(addedRules);
            removedRules = List.copyOf(removedRules);
            addedTransitions = List.copyOf(addedTransitions);
            removedTransitions = List.copyOf(removedTransitions);
            notes = List.copyOf(notes);
        }
    }

    public enum DeltaStatus {
        NEW,
        REMOVED,
        MODIFIED,
        UNCHANGED
    }
}
