package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;

import java.util.Optional;

/**
 * Port sortant pour la persistence des resultats de classification.
 */
public interface ClassificationPersistencePort {

    ClassificationResult save(String sessionId, ClassificationResult result);

    Optional<ClassificationResult> findBySessionId(String sessionId);
}
