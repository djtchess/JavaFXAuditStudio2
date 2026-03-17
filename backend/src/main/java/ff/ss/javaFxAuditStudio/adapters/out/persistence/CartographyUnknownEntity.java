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
@Table(name = "cartography_unknown")
public class CartographyUnknownEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cartography_id", nullable = false)
    private CartographyResultEntity cartography;

    @Column(name = "location", length = 512)
    private String location;

    @Column(name = "reason", length = 1024)
    private String reason;

    /** Constructeur no-arg requis par JPA. */
    protected CartographyUnknownEntity() {
    }

    public CartographyUnknownEntity(
            final CartographyResultEntity cartography,
            final String location,
            final String reason) {
        this.cartography = cartography;
        this.location = location;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public CartographyResultEntity getCartography() {
        return cartography;
    }

    public String getLocation() {
        return location;
    }

    public String getReason() {
        return reason;
    }
}
