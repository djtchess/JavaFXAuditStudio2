package ff.ss.javaFxAuditStudio.domain.workbench;

import java.time.Instant;
import java.util.Objects;

public record AnalysisSession(
        String sessionId,
        String sessionName,
        String controllerName,
        String sourceSnippetRef,
        AnalysisStatus status,
        Instant createdAt) {

    public AnalysisSession {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(sessionName, "sessionName must not be null");
        Objects.requireNonNull(controllerName, "controllerName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public AnalysisSession(
            final String sessionId,
            final String controllerName,
            final String sourceSnippetRef,
            final AnalysisStatus status,
            final Instant createdAt) {
        this(sessionId, controllerName, controllerName, sourceSnippetRef, status, createdAt);
    }

    public AnalysisSession withStatus(final AnalysisStatus nextStatus) {
        AnalysisSession updated;

        updated = new AnalysisSession(sessionId, sessionName, controllerName, sourceSnippetRef, nextStatus, createdAt);
        return updated;
    }
}
