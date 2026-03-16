package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "analysis_session")
public class AnalysisSessionEntity {

    @Id
    @Column(name = "session_id", length = 36, nullable = false)
    private String sessionId;

    @Column(name = "controller_name", length = 512, nullable = false)
    private String controllerName;

    @Column(name = "source_snippet_ref", length = 1024)
    private String sourceSnippetRef;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Constructeur no-arg requis par JPA. */
    protected AnalysisSessionEntity() {
    }

    public AnalysisSessionEntity(
            String sessionId,
            String controllerName,
            String sourceSnippetRef,
            String status,
            Instant createdAt) {
        this.sessionId = sessionId;
        this.controllerName = controllerName;
        this.sourceSnippetRef = sourceSnippetRef;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getControllerName() {
        return controllerName;
    }

    public String getSourceSnippetRef() {
        return sourceSnippetRef;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
