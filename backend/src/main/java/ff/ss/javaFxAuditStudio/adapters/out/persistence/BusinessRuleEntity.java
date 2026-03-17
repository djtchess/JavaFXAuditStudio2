package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "business_rule")
public class BusinessRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classification_id", nullable = false)
    private ClassificationResultEntity classification;

    @Column(name = "rule_id", length = 64)
    private String ruleId;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "source_ref", length = 512)
    private String sourceRef;

    @Column(name = "source_line", nullable = false)
    private int sourceLine;

    @Column(name = "responsibility_class", length = 32)
    private String responsibilityClass;

    @Column(name = "extraction_candidate", length = 32)
    private String extractionCandidate;

    @Column(name = "uncertain", nullable = false)
    private boolean uncertain;

    /** Constructeur no-arg requis par JPA. */
    protected BusinessRuleEntity() {
    }

    public BusinessRuleEntity(
            final ClassificationResultEntity classification,
            final String ruleId,
            final String description,
            final String sourceRef,
            final int sourceLine,
            final String responsibilityClass,
            final String extractionCandidate,
            final boolean uncertain) {
        this.classification = classification;
        this.ruleId = ruleId;
        this.description = description;
        this.sourceRef = sourceRef;
        this.sourceLine = sourceLine;
        this.responsibilityClass = responsibilityClass;
        this.extractionCandidate = extractionCandidate;
        this.uncertain = uncertain;
    }

    public Long getId() {
        return id;
    }

    public ClassificationResultEntity getClassification() {
        return classification;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getDescription() {
        return description;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public int getSourceLine() {
        return sourceLine;
    }

    public String getResponsibilityClass() {
        return responsibilityClass;
    }

    public String getExtractionCandidate() {
        return extractionCandidate;
    }

    public boolean isUncertain() {
        return uncertain;
    }
}
