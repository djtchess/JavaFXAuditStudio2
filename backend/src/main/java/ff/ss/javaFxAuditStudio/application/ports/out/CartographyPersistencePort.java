package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;

import java.util.Optional;

/**
 * Port sortant pour la persistence des resultats de cartographie.
 */
public interface CartographyPersistencePort {

    ControllerCartography save(String sessionId, ControllerCartography cartography);

    Optional<ControllerCartography> findBySessionId(String sessionId);
}
