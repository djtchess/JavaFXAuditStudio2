package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.workbench.OrchestratedAnalysisResult;

/**
 * Port entrant pour l'orchestration bout-en-bout d'une session d'analyse.
 * Declenche la sequence complete : ingestion, cartographie, classification,
 * plan de migration, generation, restitution.
 */
public interface AnalysisOrchestrationUseCase {

    /**
     * Orchestre le pipeline complet d'analyse pour la session identifiee.
     *
     * @param sessionId identifiant de la session a orchestrer, non null
     * @return resultat agrege de toutes les etapes, jamais null ;
     *         {@code finalStatus} vaut {@code FAILED} si une etape echoue
     */
    OrchestratedAnalysisResult orchestrate(String sessionId);
}
