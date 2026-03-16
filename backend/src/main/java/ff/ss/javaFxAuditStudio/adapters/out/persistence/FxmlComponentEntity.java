package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fxml_component")
public class FxmlComponentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cartography_id", nullable = false)
    private Long cartographyId;

    @Column(name = "fx_id", length = 256)
    private String fxId;

    @Column(name = "component_type", length = 256)
    private String componentType;

    @Column(name = "event_handler", length = 256, nullable = false)
    private String eventHandler;

    /** Constructeur no-arg requis par JPA. */
    protected FxmlComponentEntity() {
    }

    public FxmlComponentEntity(
            final Long cartographyId,
            final String fxId,
            final String componentType,
            final String eventHandler) {
        this.cartographyId = cartographyId;
        this.fxId = fxId;
        this.componentType = componentType;
        this.eventHandler = eventHandler;
    }

    public Long getId() {
        return id;
    }

    public Long getCartographyId() {
        return cartographyId;
    }

    public String getFxId() {
        return fxId;
    }

    public String getComponentType() {
        return componentType;
    }

    public String getEventHandler() {
        return eventHandler;
    }
}
