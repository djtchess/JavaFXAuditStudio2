package ff.ss.javaFxAuditStudio.domain.cartography;

import java.util.Objects;

public record HandlerBinding(
        String methodName,
        String fxmlRef,
        String injectedType) {

    public HandlerBinding {
        Objects.requireNonNull(methodName, "methodName must not be null");
        Objects.requireNonNull(fxmlRef, "fxmlRef must not be null");
        Objects.requireNonNull(injectedType, "injectedType must not be null");
    }
}
