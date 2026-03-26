package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.ai.SanitizedSourcePreviewResult;

/**
 * Port entrant pour la previsualisation du code sanitise (JAS-031).
 *
 * <p>Retourne le bundle sanitise sans appeler le LLM,
 * afin de permettre au frontend d'inspecter ce qui serait transmis.
 */
public interface PreviewSanitizedSourceUseCase {

    /**
     * Construit et retourne le bundle sanitise pour une session donnee.
     *
     * @param sessionId identifiant de la session d'analyse
     * @return resultat de previsualisation avec indicateur de sanitisation effective
     * @throws IllegalArgumentException si la session est introuvable
     */
    SanitizedSourcePreviewResult preview(String sessionId);
}
