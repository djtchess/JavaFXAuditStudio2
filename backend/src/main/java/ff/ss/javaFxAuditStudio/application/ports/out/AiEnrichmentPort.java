package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;

/**
 * Port sortant d'enrichissement IA.
 *
 * <p>Les implementations delegent vers le fournisseur IA configure.
 * Le bundle recu a deja ete sanitise — aucun code brut ne transite par ce port.
 */
public interface AiEnrichmentPort {

    /**
     * Enrichit le bundle sanitise selon la tache demandee.
     *
     * @param request requete prealablement validee avec bundle sanitise
     * @return resultat nominal ou degrade, jamais null
     */
    AiEnrichmentResult enrich(AiEnrichmentRequest request);

    /**
     * Enrichit le bundle sanitise selon la tache demandee avec un provider cible optionnel.
     *
     * @param request requete prealablement validee avec bundle sanitise
     * @param provider provider LLM cible, ou null pour conserver le provider configure
     * @return resultat nominal ou degrade, jamais null
     */
    default AiEnrichmentResult enrich(final AiEnrichmentRequest request, final LlmProvider provider) {
        return enrich(request);
    }
}
