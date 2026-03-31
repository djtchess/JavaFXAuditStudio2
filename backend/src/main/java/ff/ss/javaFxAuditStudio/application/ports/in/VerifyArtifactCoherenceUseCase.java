package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.ai.ArtifactCoherenceResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;

/**
 * Port entrant pour la verification de coherence entre les artefacts generes.
 */
public interface VerifyArtifactCoherenceUseCase {

    /**
     * Verifie la coherence inter-artefacts pour une session d'analyse.
     *
     * @param sessionId identifiant de la session
     * @return resultat de coherence, possiblement degrade
     */
    default ArtifactCoherenceResult verify(final String sessionId) {
        return verify(sessionId, null);
    }

    ArtifactCoherenceResult verify(String sessionId, LlmProvider provider);
}
