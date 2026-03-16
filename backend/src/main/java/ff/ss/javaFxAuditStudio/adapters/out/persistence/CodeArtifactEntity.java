package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "code_artifact")
public class CodeArtifactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", length = 36, nullable = false)
    private String sessionId;

    @Column(name = "controller_ref", length = 512)
    private String controllerRef;

    @Column(name = "artifact_id", length = 128, nullable = false)
    private String artifactId;

    @Column(name = "artifact_type", length = 32)
    private String artifactType;

    @Column(name = "lot_number")
    private Integer lotNumber;

    @Column(name = "class_name", length = 256)
    private String className;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "transitional_bridge", nullable = false)
    private boolean transitionalBridge;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Constructeur no-arg requis par JPA. */
    protected CodeArtifactEntity() {
    }

    public CodeArtifactEntity(
            final String sessionId,
            final String controllerRef,
            final String artifactId,
            final String artifactType,
            final Integer lotNumber,
            final String className,
            final String content,
            final boolean transitionalBridge,
            final Instant createdAt) {
        this.sessionId = sessionId;
        this.controllerRef = controllerRef;
        this.artifactId = artifactId;
        this.artifactType = artifactType;
        this.lotNumber = lotNumber;
        this.className = className;
        this.content = content;
        this.transitionalBridge = transitionalBridge;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getControllerRef() {
        return controllerRef;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public Integer getLotNumber() {
        return lotNumber;
    }

    public String getClassName() {
        return className;
    }

    public String getContent() {
        return content;
    }

    public boolean isTransitionalBridge() {
        return transitionalBridge;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
