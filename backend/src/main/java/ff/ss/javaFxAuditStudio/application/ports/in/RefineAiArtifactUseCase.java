package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.ai.AiArtifactRefinementCommand;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;

/**
 * Raffinement multi-tour d'un artefact IA déjà généré.
 */
public interface RefineAiArtifactUseCase {

    default AiCodeGenerationResult refine(final String sessionId, final AiArtifactRefinementCommand command) {
        return refine(sessionId, command, null);
    }

    AiCodeGenerationResult refine(String sessionId, AiArtifactRefinementCommand command, LlmProvider provider);
}
