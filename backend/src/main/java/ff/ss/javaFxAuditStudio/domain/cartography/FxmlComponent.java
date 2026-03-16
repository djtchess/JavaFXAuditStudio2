package ff.ss.javaFxAuditStudio.domain.cartography;

import java.util.Objects;

public record FxmlComponent(
        String fxId,
        String componentType,
        String eventHandler) {

    public FxmlComponent {
        Objects.requireNonNull(fxId, "fxId must not be null");
        Objects.requireNonNull(componentType, "componentType must not be null");
        if (eventHandler == null) {
            eventHandler = "";
        }
    }
}
