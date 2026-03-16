package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.cartography.FxmlComponent;
import ff.ss.javaFxAuditStudio.domain.cartography.HandlerBinding;

import java.util.List;

public interface CartographyAnalysisPort {

    List<FxmlComponent> extractComponents(String fxmlContent);

    List<HandlerBinding> extractHandlers(String javaContent);
}
