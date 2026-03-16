package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.List;
import java.util.Objects;

public record RestitutionReportResponse(
        String controllerRef,
        int ruleCount,
        int artifactCount,
        String confidence,
        boolean isActionable,
        List<String> findings,
        List<String> unknowns) {

    public RestitutionReportResponse {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(confidence, "confidence must not be null");
        Objects.requireNonNull(findings, "findings must not be null");
        Objects.requireNonNull(unknowns, "unknowns must not be null");
        findings = List.copyOf(findings);
        unknowns = List.copyOf(unknowns);
    }
}
