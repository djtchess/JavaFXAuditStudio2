package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.List;
import java.util.Objects;

public record CartographyResponse(
        String controllerRef,
        String fxmlRef,
        List<FxmlComponentDto> components,
        List<HandlerBindingDto> handlers,
        boolean hasUnknowns) {

    public CartographyResponse {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(fxmlRef, "fxmlRef must not be null");
        Objects.requireNonNull(components, "components must not be null");
        Objects.requireNonNull(handlers, "handlers must not be null");
        components = List.copyOf(components);
        handlers = List.copyOf(handlers);
    }

    public record FxmlComponentDto(
            String fxId,
            String componentType,
            String eventHandler) {

        public FxmlComponentDto {
            Objects.requireNonNull(fxId, "fxId must not be null");
            Objects.requireNonNull(componentType, "componentType must not be null");
            Objects.requireNonNull(eventHandler, "eventHandler must not be null");
        }
    }

    public record HandlerBindingDto(
            String methodName,
            String fxmlRef,
            String injectedType) {

        public HandlerBindingDto {
            Objects.requireNonNull(methodName, "methodName must not be null");
            Objects.requireNonNull(fxmlRef, "fxmlRef must not be null");
            Objects.requireNonNull(injectedType, "injectedType must not be null");
        }
    }
}
