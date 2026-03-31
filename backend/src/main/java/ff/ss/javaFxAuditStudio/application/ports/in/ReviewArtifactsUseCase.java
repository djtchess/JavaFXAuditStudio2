package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.ai.ArtifactReviewResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;

/**
 * Port entrant pour la revue IA des artefacts generes (JAS-030).
 */
public interface ReviewArtifactsUseCase {

    /**
     * Lance une revue IA sur les regles classifiees d'une session.
     *
     * @param sessionId identifiant de la session d'analyse
     * @return resultat de la revue (nominal ou degrade)
     * @throws IllegalArgumentException si la session est introuvable
     */
    default ArtifactReviewResult review(final String sessionId) {
        return review(sessionId, null);
    }

    ArtifactReviewResult review(String sessionId, LlmProvider provider);
}
