package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.List;
import java.util.Objects;

public record ClassificationResponse(
        String controllerRef,
        int ruleCount,
        int uncertainCount,
        List<BusinessRuleDto> rules) {

    public ClassificationResponse {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        rules = List.copyOf(rules);
    }

    public record BusinessRuleDto(
            String ruleId,
            String description,
            String responsibilityClass,
            String extractionCandidate,
            boolean uncertain) {

        public BusinessRuleDto {
            Objects.requireNonNull(ruleId, "ruleId must not be null");
            Objects.requireNonNull(description, "description must not be null");
            Objects.requireNonNull(responsibilityClass, "responsibilityClass must not be null");
            Objects.requireNonNull(extractionCandidate, "extractionCandidate must not be null");
        }
    }
}
