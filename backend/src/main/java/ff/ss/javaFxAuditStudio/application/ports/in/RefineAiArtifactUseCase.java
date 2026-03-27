package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.ai.AiArtifactRefinementCommand;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;

/**
 * Raffinement multi-tour d'un artefact IA déjà généré.
 */
public interface RefineAiArtifactUseCase {

    AiCodeGenerationResult refine(String sessionId, AiArtifactRefinementCommand command);
}
