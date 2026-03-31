package ff.ss.javaFxAuditStudio.domain.workbench;

import java.time.Instant;
import java.util.Objects;

public record AnalysisStatusTransition(
        String sessionId,
        AnalysisStatus status,
        Instant occurredAt) {

    public AnalysisStatusTransition {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
