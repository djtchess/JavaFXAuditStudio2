package ff.ss.javaFxAuditStudio.adapters.in.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.CartographyResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.CartographyResponse.FxmlComponentDto;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.CartographyResponse.HandlerBindingDto;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.cartography.FxmlComponent;
import ff.ss.javaFxAuditStudio.domain.cartography.HandlerBinding;

@Component
public class CartographyResponseMapper {

    public CartographyResponse toResponse(final ControllerCartography cartography) {
        return new CartographyResponse(
                cartography.controllerRef(),
                cartography.fxmlRef(),
                mapComponents(cartography.components()),
                mapHandlers(cartography.handlers()),
                cartography.hasUnknowns());
    }

    private List<FxmlComponentDto> mapComponents(final List<FxmlComponent> components) {
        return components.stream()
                .map(c -> new FxmlComponentDto(c.fxId(), c.componentType(), c.eventHandler()))
                .toList();
    }

    private List<HandlerBindingDto> mapHandlers(final List<HandlerBinding> handlers) {
        return handlers.stream()
                .map(h -> new HandlerBindingDto(h.methodName(), h.fxmlRef(), h.injectedType()))
                .toList();
    }
}
