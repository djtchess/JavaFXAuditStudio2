package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_reference_pattern")
public class ProjectReferencePatternEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "artifact_type", length = 64, nullable = false)
    private String artifactType;

    @Column(name = "reference_name", length = 255, nullable = false)
    private String referenceName;

    @Column(name = "content", columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProjectReferencePatternEntity() {
    }

    public ProjectReferencePatternEntity(
            final String id,
            final String artifactType,
            final String referenceName,
            final String content,
            final Instant createdAt) {
        this.id = id;
        this.artifactType = artifactType;
        this.referenceName = referenceName;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
