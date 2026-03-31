package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.ai.AiGeneratedArtifact;
import ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import java.util.List;

/**
 * Port sortant de sanitisation du contexte promptable (AI-5).
 * Sanitise les fragments de code et le texte libre avant injection dans les prompts LLM.
 */
public interface PromptContextSanitizerPort {

    /**
     * Sanitise un fragment de code (previousCode, currentArtifactCode, etc.)
     * via le pipeline de sanitisation.
     * @param requestId    identifiant de correlation
     * @param rawCode      code brut a sanitiser
     * @param contextLabel label pour la tracabilite (ex: "previousCode")
     * @return code sanitise, ou chaine vide si rawCode est blank
     */
    default String sanitizeCodeFragment(
            final String requestId,
            final String rawCode,
            final String contextLabel) {
        return sanitizeCodeFragment(requestId, null, rawCode, contextLabel);
    }

    String sanitizeCodeFragment(String requestId, TaskType taskType, String rawCode, String contextLabel);

    /**
     * Sanitise une instruction utilisateur : troncature + rejet des marqueurs d'injection.
     * @param instruction texte brut de l'instruction
     * @param maxLength   longueur maximale autorisee
     * @return instruction nettoyee
     */
    default String sanitizeInstruction(final String instruction, final int maxLength) {
        return sanitizeInstruction(null, null, instruction, maxLength);
    }

    String sanitizeInstruction(String requestId, TaskType taskType, String instruction, int maxLength);

    /**
     * Sanitise le contenu des artefacts IA avant injection dans un prompt de coherence.
     * @param requestId identifiant de correlation
     * @param artifacts liste des artefacts IA generes
     * @return chaine formatee avec contenus sanitises
     */
    default String sanitizeArtifactDetails(final String requestId, final List<AiGeneratedArtifact> artifacts) {
        return sanitizeArtifactDetails(requestId, null, artifacts);
    }

    String sanitizeArtifactDetails(String requestId, TaskType taskType, List<AiGeneratedArtifact> artifacts);

    /**
     * Sanitise les patterns projet avant injection dans les prompts de generation et coherence.
     * @param requestId identifiant de correlation
     * @param patterns  liste des patterns de reference
     * @return chaine formatee avec contenus sanitises
     */
    default String sanitizeReferencePatterns(final String requestId, final List<ProjectReferencePattern> patterns) {
        return sanitizeReferencePatterns(requestId, null, patterns);
    }

    String sanitizeReferencePatterns(String requestId, TaskType taskType, List<ProjectReferencePattern> patterns);
}
