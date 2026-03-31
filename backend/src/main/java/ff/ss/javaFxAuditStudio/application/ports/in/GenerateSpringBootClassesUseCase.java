package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;

/**
 * Port entrant pour la génération IA des classes cibles Spring Boot (JAS-031).
 *
 * <p>Le LLM reçoit le code source sanitisé du contrôleur JavaFX ainsi que les
 * règles classifiées, et génère les classes Spring Boot cibles correspondantes
 * (UseCase, ViewModel, Policy, Gateway) en code Java compilable.
 */
public interface GenerateSpringBootClassesUseCase {

    /**
     * Génère les classes Spring Boot cibles pour la session d'analyse donnée.
     *
     * <p>La génération est dégradée (résultat vide) si :
     * <ul>
     *   <li>La session est introuvable.</li>
     *   <li>Aucune classification n'est disponible pour la session.</li>
     *   <li>La sanitisation refuse la source (marqueur sensible détecté).</li>
     *   <li>Le fournisseur IA est indisponible (circuit ouvert ou erreur réseau).</li>
     * </ul>
     *
     * @param sessionId identifiant de la session d'analyse
     * @return résultat contenant les classes générées par type d'artefact,
     *         ou un résultat dégradé si la génération a échoué
     * @throws IllegalArgumentException si la session est introuvable
     */
    default AiCodeGenerationResult generate(final String sessionId) {
        return generate(sessionId, null);
    }

    AiCodeGenerationResult generate(String sessionId, LlmProvider provider);
}
