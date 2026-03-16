package ff.ss.javaFxAuditStudio.domain.cartography;

import java.util.List;
import java.util.Objects;

public record ControllerCartography(
        String controllerRef,
        String fxmlRef,
        List<FxmlComponent> components,
        List<HandlerBinding> handlers,
        List<CartographyUnknown> unknowns) {

    public ControllerCartography {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(fxmlRef, "fxmlRef must not be null");
        components = List.copyOf(components);
        handlers = List.copyOf(handlers);
        unknowns = List.copyOf(unknowns);
    }

    public boolean hasUnknowns() {
        return !unknowns.isEmpty();
    }
}
