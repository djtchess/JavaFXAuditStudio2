package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;

public interface CartographyUseCase {

    ControllerCartography handle(String sessionId, String controllerRef, String fxmlRef);
}
