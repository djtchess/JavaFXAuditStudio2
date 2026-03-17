package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cartography_result")
public class CartographyResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", length = 36, nullable = false, unique = true)
    private String sessionId;

    @Column(name = "controller_ref", length = 512)
    private String controllerRef;

    @Column(name = "fxml_ref", length = 512)
    private String fxmlRef;

    @Column(name = "has_unknowns", nullable = false)
    private boolean hasUnknowns;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "cartography", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FxmlComponentEntity> components = new ArrayList<>();

    @OneToMany(mappedBy = "cartography", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<HandlerBindingEntity> handlers = new ArrayList<>();

    @OneToMany(mappedBy = "cartography", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartographyUnknownEntity> unknowns = new ArrayList<>();

    /** Constructeur no-arg requis par JPA. */
    protected CartographyResultEntity() {
    }

    public CartographyResultEntity(
            final String sessionId,
            final String controllerRef,
            final String fxmlRef,
            final boolean hasUnknowns,
            final Instant createdAt) {
        this.sessionId = sessionId;
        this.controllerRef = controllerRef;
        this.fxmlRef = fxmlRef;
        this.hasUnknowns = hasUnknowns;
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

    public String getFxmlRef() {
        return fxmlRef;
    }

    public boolean isHasUnknowns() {
        return hasUnknowns;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<FxmlComponentEntity> getComponents() {
        return components;
    }

    public List<HandlerBindingEntity> getHandlers() {
        return handlers;
    }

    public List<CartographyUnknownEntity> getUnknowns() {
        return unknowns;
    }
}
