package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "regression_risk")
public class RegressionRiskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lot_id", nullable = false)
    private Long lotId;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "risk_level", length = 16)
    private String riskLevel;

    @Column(name = "mitigation", columnDefinition = "text")
    private String mitigation;

    /** Constructeur no-arg requis par JPA. */
    protected RegressionRiskEntity() {
    }

    public RegressionRiskEntity(
            final Long lotId,
            final String description,
            final String riskLevel,
            final String mitigation) {
        this.lotId = lotId;
        this.description = description;
        this.riskLevel = riskLevel;
        this.mitigation = mitigation;
    }

    public Long getId() {
        return id;
    }

    public Long getLotId() {
        return lotId;
    }

    public String getDescription() {
        return description;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getMitigation() {
        return mitigation;
    }
}
