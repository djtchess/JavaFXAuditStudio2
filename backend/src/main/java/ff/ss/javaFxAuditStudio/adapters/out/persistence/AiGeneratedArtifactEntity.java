package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_generated_artifact")
public class AiGeneratedArtifactEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "session_id", length = 36, nullable = false)
    private String sessionId;

    @Column(name = "artifact_type", length = 64, nullable = false)
    private String artifactType;

    @Column(name = "class_name", length = 255, nullable = false)
    private String className;

    @Column(name = "content", columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "parent_version_id", length = 36)
    private String parentVersionId;

    @Column(name = "request_id", length = 36, nullable = false)
    private String requestId;

    @Column(name = "provider", length = 64, nullable = false)
    private String provider;

    @Column(name = "origin_task", length = 64, nullable = false)
    private String originTask;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AiGeneratedArtifactEntity() {
    }

    public AiGeneratedArtifactEntity(
            final String id,
            final String sessionId,
            final String artifactType,
            final String className,
            final String content,
            final Integer versionNumber,
            final String parentVersionId,
            final String requestId,
            final String provider,
            final String originTask,
            final Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.artifactType = artifactType;
        this.className = className;
        this.content = content;
        this.versionNumber = versionNumber;
        this.parentVersionId = parentVersionId;
        this.requestId = requestId;
        this.provider = provider;
        this.originTask = originTask;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public String getClassName() {
        return className;
    }

    public String getContent() {
        return content;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public String getParentVersionId() {
        return parentVersionId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getProvider() {
        return provider;
    }

    public String getOriginTask() {
        return originTask;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
