package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cartography_unknown")
public class CartographyUnknownEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cartography_id", nullable = false)
    private Long cartographyId;

    @Column(name = "location", length = 512)
    private String location;

    @Column(name = "reason", length = 1024)
    private String reason;

    /** Constructeur no-arg requis par JPA. */
    protected CartographyUnknownEntity() {
    }

    public CartographyUnknownEntity(
            final Long cartographyId,
            final String location,
            final String reason) {
        this.cartographyId = cartographyId;
        this.location = location;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public Long getCartographyId() {
        return cartographyId;
    }

    public String getLocation() {
        return location;
    }

    public String getReason() {
        return reason;
    }
}
