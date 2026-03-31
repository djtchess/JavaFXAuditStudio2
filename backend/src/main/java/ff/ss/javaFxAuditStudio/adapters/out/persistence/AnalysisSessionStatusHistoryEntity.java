package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "analysis_session_status_history")
public class AnalysisSessionStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", length = 36, nullable = false)
    private String sessionId;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AnalysisSessionStatusHistoryEntity() {
    }

    public AnalysisSessionStatusHistoryEntity(
            final String sessionId,
            final String status,
            final Instant occurredAt) {
        this.sessionId = sessionId;
        this.status = status;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
