package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationReport;

/**
 * Port sortant de sanitisation (JAS-018).
 *
 * <p>Les implementations desensibilisent la source brute avant tout appel LLM.
 * Ce port est invoque par {@code EnrichAnalysisService} avant la construction
 * de l'{@code AiEnrichmentRequest}.
 */
public interface SanitizationPort {

    /**
     * Sanitise la source brute et retourne un {@link SanitizedBundle} pret pour l'appel LLM.
     *
     * <p>Limites et contrat :
     * <ul>
     *   <li>Un seul fichier Java par appel. Ne pas aggreger plusieurs sources dans {@code rawSource}.</li>
     *   <li>{@code rawSource} ne doit pas etre blank (null, vide ou compose uniquement d'espaces).
     *       Un {@link IllegalArgumentException} est lance si cette condition n'est pas respectee.</li>
     *   <li>{@code bundleId} est un identifiant de session genere par l'appelant pour assurer
     *       la tracabilite du bundle a travers le pipeline ; il doit etre unique par invocation.</li>
     * </ul>
     *
     * @param bundleId      Identifiant de tracabilite du bundle (unique par invocation, non null)
     * @param rawSource     Code source brut a desensibiliser (non null, non blank, un seul fichier)
     * @param controllerRef Reference au controller (nom ou chemin, non null)
     * @return bundle sanitise, jamais null
     * @throws IllegalArgumentException     si {@code rawSource} est blank
     * @throws SanitizationRefusedException si un marqueur sensible subsiste apres sanitisation
     *                                       ou si le nombre de tokens depasse le plafond
     */
    SanitizedBundle sanitize(String bundleId, String rawSource, String controllerRef);

    /**
     * Previsualise les transformations du pipeline sans lever de {@link SanitizationRefusedException}.
     *
     * <p>Toutes les regles de sanitisation sont executees en sequence et leurs
     * transformations collectees. Le detecteur de marqueurs sensibles est consulte en
     * mode observation uniquement : son resultat est consigne dans le rapport mais
     * aucun refus n'est leve. Aucun appel LLM n'est effectue.
     *
     * @param bundleId      Identifiant de tracabilite du bundle (non null)
     * @param rawSource     Code source brut a inspecter (non null)
     * @param controllerRef Reference au controller (nom ou chemin, non null)
     * @return rapport complet des transformations detectees, jamais null
     */
    SanitizationReport previewTransformations(String bundleId, String rawSource, String controllerRef);
}
