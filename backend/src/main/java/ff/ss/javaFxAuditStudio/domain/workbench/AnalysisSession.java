package ff.ss.javaFxAuditStudio.domain.workbench;

import java.time.Instant;
import java.util.Objects;

public record AnalysisSession(
        String sessionId,
        String controllerName,
        String sourceSnippetRef,
        AnalysisStatus status,
        Instant createdAt) {

    public AnalysisSession {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(controllerName, "controllerName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
