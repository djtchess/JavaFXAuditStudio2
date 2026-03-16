package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "restitution_report")
public class RestitutionReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", length = 36, nullable = false, unique = true)
    private String sessionId;

    @Column(name = "controller_ref", length = 512)
    private String controllerRef;

    @Column(name = "rule_count", nullable = false)
    private int ruleCount;

    @Column(name = "uncertain_count", nullable = false)
    private int uncertainCount;

    @Column(name = "artifact_count", nullable = false)
    private int artifactCount;

    @Column(name = "bridge_count", nullable = false)
    private int bridgeCount;

    @Column(name = "confidence", length = 16)
    private String confidence;

    @Column(name = "has_contradictions", nullable = false)
    private boolean hasContradictions;

    @Column(name = "contradictions", columnDefinition = "text")
    @Convert(converter = StringListJsonbConverter.class)
    private List<String> contradictions;

    @Column(name = "unknowns", columnDefinition = "text")
    @Convert(converter = StringListJsonbConverter.class)
    private List<String> unknowns;

    @Column(name = "findings", columnDefinition = "text")
    @Convert(converter = StringListJsonbConverter.class)
    private List<String> findings;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Constructeur no-arg requis par JPA. */
    protected RestitutionReportEntity() {
    }

    public RestitutionReportEntity(
            final String sessionId,
            final String controllerRef,
            final int ruleCount,
            final int uncertainCount,
            final int artifactCount,
            final int bridgeCount,
            final String confidence,
            final boolean hasContradictions,
            final List<String> contradictions,
            final List<String> unknowns,
            final List<String> findings,
            final Instant createdAt) {
        this.sessionId = sessionId;
        this.controllerRef = controllerRef;
        this.ruleCount = ruleCount;
        this.uncertainCount = uncertainCount;
        this.artifactCount = artifactCount;
        this.bridgeCount = bridgeCount;
        this.confidence = confidence;
        this.hasContradictions = hasContradictions;
        this.contradictions = contradictions;
        this.unknowns = unknowns;
        this.findings = findings;
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

    public int getRuleCount() {
        return ruleCount;
    }

    public int getUncertainCount() {
        return uncertainCount;
    }

    public int getArtifactCount() {
        return artifactCount;
    }

    public int getBridgeCount() {
        return bridgeCount;
    }

    public String getConfidence() {
        return confidence;
    }

    public boolean isHasContradictions() {
        return hasContradictions;
    }

    public List<String> getContradictions() {
        return contradictions;
    }

    public List<String> getUnknowns() {
        return unknowns;
    }

    public List<String> getFindings() {
        return findings;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
