package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.ai.AiGeneratedArtifact;
import ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern;
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
    String sanitizeCodeFragment(String requestId, String rawCode, String contextLabel);

    /**
     * Sanitise une instruction utilisateur : troncature + rejet des marqueurs d'injection.
     * @param instruction texte brut de l'instruction
     * @param maxLength   longueur maximale autorisee
     * @return instruction nettoyee
     */
    String sanitizeInstruction(String instruction, int maxLength);

    /**
     * Sanitise le contenu des artefacts IA avant injection dans un prompt de coherence.
     * @param requestId identifiant de correlation
     * @param artifacts liste des artefacts IA generes
     * @return chaine formatee avec contenus sanitises
     */
    String sanitizeArtifactDetails(String requestId, List<AiGeneratedArtifact> artifacts);

    /**
     * Sanitise les patterns projet avant injection dans les prompts de generation et coherence.
     * @param requestId identifiant de correlation
     * @param patterns  liste des patterns de reference
     * @return chaine formatee avec contenus sanitises
     */
    String sanitizeReferencePatterns(String requestId, List<ProjectReferencePattern> patterns);
}
