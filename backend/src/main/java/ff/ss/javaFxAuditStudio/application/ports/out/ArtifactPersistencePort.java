package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;

import java.util.Optional;

/**
 * Port sortant pour la persistence des resultats de generation d'artefacts.
 */
public interface ArtifactPersistencePort {

    GenerationResult save(String sessionId, GenerationResult result);

    Optional<GenerationResult> findBySessionId(String sessionId);
}
