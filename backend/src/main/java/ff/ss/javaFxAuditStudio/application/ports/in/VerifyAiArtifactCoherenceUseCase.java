package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.ai.AiArtifactCoherenceResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;

/**
 * Vérification de cohérence inter-artefacts sur les sorties IA persistées.
 */
public interface VerifyAiArtifactCoherenceUseCase {

    default AiArtifactCoherenceResult verify(final String sessionId) {
        return verify(sessionId, null);
    }

    AiArtifactCoherenceResult verify(String sessionId, LlmProvider provider);
}
