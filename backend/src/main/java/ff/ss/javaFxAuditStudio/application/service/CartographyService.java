package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.CartographyUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyAnalysisPort;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyPersistencePort;
import ff.ss.javaFxAuditStudio.domain.cartography.CartographyUnknown;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CartographyService implements CartographyUseCase {

    private static final Logger log = LoggerFactory.getLogger(CartographyService.class);

    private final CartographyAnalysisPort cartographyAnalysisPort;
    private final CartographyPersistencePort cartographyPersistencePort;

    public CartographyService(
            final CartographyAnalysisPort cartographyAnalysisPort,
            final CartographyPersistencePort cartographyPersistencePort) {
        this.cartographyAnalysisPort = Objects.requireNonNull(
                cartographyAnalysisPort, "cartographyAnalysisPort must not be null");
        this.cartographyPersistencePort = Objects.requireNonNull(
                cartographyPersistencePort, "cartographyPersistencePort must not be null");
    }

    @Override
    public ControllerCartography handle(
            final String sessionId,
            final String controllerRef,
            final String fxmlRef) {
        log.debug("Cartographie demarree - controllerRef masquee");

        Optional<ControllerCartography> cached = cartographyPersistencePort.findBySessionId(sessionId);
        if (cached.isPresent()) {
            log.debug("Cartographie trouvee en cache pour la session");
            return cached.get();
        }

        CartographyUnknown pending = new CartographyUnknown(
                "JAS-31",
                "Analyse non encore connectee a l'ingestion");
        ControllerCartography result = new ControllerCartography(
                controllerRef,
                fxmlRef == null ? "" : fxmlRef,
                List.of(),
                List.of(),
                List.of(pending));

        cartographyPersistencePort.save(sessionId, result);

        log.debug("Cartographie terminee - {} composants, {} handlers, {} inconnues",
                result.components().size(), result.handlers().size(), result.unknowns().size());
        return result;
    }
}
