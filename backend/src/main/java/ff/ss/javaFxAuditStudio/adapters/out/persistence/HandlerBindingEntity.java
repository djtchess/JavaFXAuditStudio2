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
@Table(name = "handler_binding")
public class HandlerBindingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cartography_id", nullable = false)
    private CartographyResultEntity cartography;

    @Column(name = "method_name", length = 256)
    private String methodName;

    @Column(name = "fxml_ref", length = 512)
    private String fxmlRef;

    @Column(name = "injected_type", length = 256)
    private String injectedType;

    /** Constructeur no-arg requis par JPA. */
    protected HandlerBindingEntity() {
    }

    public HandlerBindingEntity(
            final CartographyResultEntity cartography,
            final String methodName,
            final String fxmlRef,
            final String injectedType) {
        this.cartography = cartography;
        this.methodName = methodName;
        this.fxmlRef = fxmlRef;
        this.injectedType = injectedType;
    }

    public Long getId() {
        return id;
    }

    public CartographyResultEntity getCartography() {
        return cartography;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getFxmlRef() {
        return fxmlRef;
    }

    public String getInjectedType() {
        return injectedType;
    }
}
