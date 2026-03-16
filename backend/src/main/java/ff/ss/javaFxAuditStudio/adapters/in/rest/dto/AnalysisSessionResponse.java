package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.Objects;

public record AnalysisSessionResponse(
        String sessionId,
        String status,
        String sessionName,
        String controllerRef) {

    public AnalysisSessionResponse {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(sessionName, "sessionName must not be null");
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
    }
}
