package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Convert;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "planned_lot")
public class PlannedLotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "lot_number", nullable = false)
    private int lotNumber;

    @Column(name = "title", length = 256)
    private String title;

    @Column(name = "objective", columnDefinition = "text")
    private String objective;

    @Column(name = "extraction_candidates", columnDefinition = "text", nullable = false)
    @Convert(converter = StringListJsonbConverter.class)
    private List<String> extractionCandidates;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "lot_id")
    private List<RegressionRiskEntity> risks = new ArrayList<>();

    /** Constructeur no-arg requis par JPA. */
    protected PlannedLotEntity() {
    }

    public PlannedLotEntity(
            final Long planId,
            final int lotNumber,
            final String title,
            final String objective,
            final List<String> extractionCandidates,
            final List<RegressionRiskEntity> risks) {
        this.planId = planId;
        this.lotNumber = lotNumber;
        this.title = title;
        this.objective = objective;
        this.extractionCandidates = extractionCandidates;
        this.risks = risks;
    }

    public Long getId() {
        return id;
    }

    public Long getPlanId() {
        return planId;
    }

    public int getLotNumber() {
        return lotNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getObjective() {
        return objective;
    }

    public List<String> getExtractionCandidates() {
        return extractionCandidates;
    }

    public List<RegressionRiskEntity> getRisks() {
        return risks;
    }
}
