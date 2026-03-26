package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.ai.SanitizedSourcePreviewResult;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationReport;

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

    /**
     * Evalue les transformations du pipeline en mode dry-run pour une session donnee.
     *
     * <p>Aucune {@link ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException}
     * n'est levee. Aucun appel LLM n'est effectue. La source n'est jamais transmise a un tiers.
     *
     * @param sessionId identifiant de la session d'analyse
     * @return rapport des transformations detectees, sans refus, jamais null
     * @throws IllegalArgumentException si la session est introuvable
     */
    SanitizationReport previewDryRun(String sessionId);
}
