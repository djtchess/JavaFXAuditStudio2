package ff.ss.javaFxAuditStudio.domain.cartography;

import java.util.Objects;

public record CartographyUnknown(
        String location,
        String reason) {

    public CartographyUnknown {
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
    }
}
