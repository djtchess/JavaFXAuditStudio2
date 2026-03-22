package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entite JPA pour la table rule_classification_audit.
 * Chaque ligne represente une reclassification manuelle d'une regle.
 */
@Entity
@Table(name = "rule_classification_audit")
public class RuleClassificationAuditEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "analysis_id", length = 255, nullable = false)
    private String analysisId;

    @Column(name = "rule_id", length = 255, nullable = false)
    private String ruleId;

    @Column(name = "from_category", length = 50, nullable = false)
    private String fromCategory;

    @Column(name = "to_category", length = 50, nullable = false)
    private String toCategory;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Constructeur no-arg requis par JPA. */
    protected RuleClassificationAuditEntity() {
    }

    public RuleClassificationAuditEntity(
            final String id,
            final String analysisId,
            final String ruleId,
            final String fromCategory,
            final String toCategory,
            final String reason,
            final Instant createdAt) {
        this.id = id;
        this.analysisId = analysisId;
        this.ruleId = ruleId;
        this.fromCategory = fromCategory;
        this.toCategory = toCategory;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getFromCategory() {
        return fromCategory;
    }

    public String getToCategory() {
        return toCategory;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
