package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;

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
     * @param bundleId      Identifiant de tracabilite du bundle
     * @param rawSource     Code source brut a desensibiliser
     * @param controllerRef Reference au controller (nom, chemin)
     * @return bundle sanitise, jamais null
     * @throws SanitizationRefusedException si un marqueur sensible subsiste apres sanitisation
     *                                       ou si le nombre de tokens depasse le plafond
     */
    SanitizedBundle sanitize(String bundleId, String rawSource, String controllerRef);
}
