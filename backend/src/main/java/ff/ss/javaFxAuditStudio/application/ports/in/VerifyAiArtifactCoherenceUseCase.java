package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.ai.AiArtifactCoherenceResult;

/**
 * Vérification de cohérence inter-artefacts sur les sorties IA persistées.
 */
public interface VerifyAiArtifactCoherenceUseCase {

    AiArtifactCoherenceResult verify(String sessionId);
}
