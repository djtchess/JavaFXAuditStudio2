package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.CartographyUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyAnalysisPort;
import ff.ss.javaFxAuditStudio.domain.cartography.CartographyUnknown;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public final class CartographyService implements CartographyUseCase {

    private static final Logger log = LoggerFactory.getLogger(CartographyService.class);

    private final CartographyAnalysisPort cartographyAnalysisPort;

    public CartographyService(final CartographyAnalysisPort cartographyAnalysisPort) {
        this.cartographyAnalysisPort = Objects.requireNonNull(
                cartographyAnalysisPort, "cartographyAnalysisPort must not be null");
    }

    @Override
    public ControllerCartography handle(final String controllerRef, final String fxmlRef) {
        log.debug("Cartographie demarree - controllerRef masquee");
        CartographyUnknown pending;
        ControllerCartography result;

        pending = new CartographyUnknown(
                "JAS-31",
                "Analyse non encore connectée à l'ingestion");
        result = new ControllerCartography(
                controllerRef,
                fxmlRef == null ? "" : fxmlRef,
                List.of(),
                List.of(),
                List.of(pending));
        log.debug("Cartographie terminee - {} composants, {} handlers, {} inconnues",
                result.components().size(), result.handlers().size(), result.unknowns().size());
        return result;
    }
}
