package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;
import ff.ss.javaFxAuditStudio.domain.ai.ArtifactRefineRequest;

/**
 * Port entrant pour le raffinement d'un artefact Spring Boot genere.
 */
public interface RefineArtifactUseCase {

    /**
     * Raffine un artefact existant pour une session d'analyse donnee.
     *
     * @param sessionId identifiant de la session
     * @param request   consigne de raffinement
     * @return resultat de generation IA, possiblement degrade
     */
    AiCodeGenerationResult refine(String sessionId, ArtifactRefineRequest request);
}
