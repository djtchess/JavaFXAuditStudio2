package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.CartographyUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyAnalysisPort;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceReaderPort;
import ff.ss.javaFxAuditStudio.domain.cartography.CartographyUnknown;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.cartography.FxmlComponent;
import ff.ss.javaFxAuditStudio.domain.cartography.HandlerBinding;
import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CartographyService implements CartographyUseCase {

    private static final Logger log = LoggerFactory.getLogger(CartographyService.class);

    private final CartographyAnalysisPort cartographyAnalysisPort;
    private final CartographyPersistencePort cartographyPersistencePort;
    private final SourceReaderPort sourceReaderPort;

    public CartographyService(
            final CartographyAnalysisPort cartographyAnalysisPort,
            final CartographyPersistencePort cartographyPersistencePort,
            final SourceReaderPort sourceReaderPort) {
        this.cartographyAnalysisPort = Objects.requireNonNull(cartographyAnalysisPort);
        this.cartographyPersistencePort = Objects.requireNonNull(cartographyPersistencePort);
        this.sourceReaderPort = Objects.requireNonNull(sourceReaderPort);
    }

    @Override
    public ControllerCartography handle(
            final String sessionId,
            final String controllerRef,
            final String fxmlRef) {
        log.debug("Cartographie demarree - session={}", sessionId);

        Optional<ControllerCartography> cached = cartographyPersistencePort.findBySessionId(sessionId);
        if (cached.isPresent()) {
            log.debug("Cartographie trouvee en cache pour la session");
            return cached.get();
        }

        List<CartographyUnknown> unknowns = new ArrayList<>();

        String fxmlContent = readSource(fxmlRef, "fxml-not-found", unknowns);
        String javaContent = readSource(controllerRef, "controller-not-found", unknowns);

        List<FxmlComponent> components = cartographyAnalysisPort.extractComponents(fxmlContent);
        List<HandlerBinding> handlers = cartographyAnalysisPort.extractHandlers(javaContent);
        unknowns.addAll(cartographyAnalysisPort.extractUnknowns(fxmlContent));

        ControllerCartography result = new ControllerCartography(
                controllerRef,
                fxmlRef == null ? "" : fxmlRef,
                components,
                handlers,
                List.copyOf(unknowns));

        cartographyPersistencePort.save(sessionId, result);

        log.debug("Cartographie terminee - {} composants, {} handlers, {} inconnues",
                components.size(), handlers.size(), unknowns.size());
        return result;
    }

    private String readSource(final String ref, final String unknownLocation,
                              final List<CartographyUnknown> unknowns) {
        if (ref == null || ref.isBlank()) {
            return "";
        }
        Optional<SourceInput> input = sourceReaderPort.read(ref);
        if (input.isEmpty()) {
            log.warn("Source introuvable - ref={}", ref);
            unknowns.add(new CartographyUnknown(unknownLocation, ref));
            return "";
        }
        return input.get().content();
    }
}
