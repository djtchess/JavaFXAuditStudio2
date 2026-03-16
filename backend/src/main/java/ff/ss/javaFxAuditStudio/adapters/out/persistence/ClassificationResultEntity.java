package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "classification_result")
public class ClassificationResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", length = 36, nullable = false, unique = true)
    private String sessionId;

    @Column(name = "controller_ref", length = 512)
    private String controllerRef;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "classification_id")
    private List<BusinessRuleEntity> rules = new ArrayList<>();

    /** Constructeur no-arg requis par JPA. */
    protected ClassificationResultEntity() {
    }

    public ClassificationResultEntity(
            final String sessionId,
            final String controllerRef,
            final Instant createdAt,
            final List<BusinessRuleEntity> rules) {
        this.sessionId = sessionId;
        this.controllerRef = controllerRef;
        this.createdAt = createdAt;
        this.rules = rules;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<BusinessRuleEntity> getRules() {
        return rules;
    }
}
