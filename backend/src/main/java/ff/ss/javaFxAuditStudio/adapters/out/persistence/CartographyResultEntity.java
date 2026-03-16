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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cartography_id")
    private List<FxmlComponentEntity> components = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cartography_id")
    private List<HandlerBindingEntity> handlers = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cartography_id")
    private List<CartographyUnknownEntity> unknowns = new ArrayList<>();

    /** Constructeur no-arg requis par JPA. */
    protected CartographyResultEntity() {
    }

    public CartographyResultEntity(
            final String sessionId,
            final String controllerRef,
            final String fxmlRef,
            final boolean hasUnknowns,
            final Instant createdAt,
            final List<FxmlComponentEntity> components,
            final List<HandlerBindingEntity> handlers,
            final List<CartographyUnknownEntity> unknowns) {
        this.sessionId = sessionId;
        this.controllerRef = controllerRef;
        this.fxmlRef = fxmlRef;
        this.hasUnknowns = hasUnknowns;
        this.createdAt = createdAt;
        this.components = components;
        this.handlers = handlers;
        this.unknowns = unknowns;
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
