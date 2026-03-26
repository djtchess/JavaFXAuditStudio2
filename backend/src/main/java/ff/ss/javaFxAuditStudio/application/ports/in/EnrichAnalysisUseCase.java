package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;

/**
 * Port entrant d'enrichissement d'une session d'analyse (IAP-2).
 *
 * <p>Le service applicatif charge la session, construit le bundle sanitisé
 * et délègue au port sortant {@code AiEnrichmentPort}.
 */
public interface EnrichAnalysisUseCase {

    /**
     * Enrichit la session identifiée par {@code sessionId}.
     *
     * @param sessionId identifiant de la session d'analyse
     * @param taskType  type de tâche IA (typesafe)
     * @return résultat nominal ou dégradé, jamais null
     * @throws IllegalArgumentException si la session est introuvable
     */
    AiEnrichmentResult enrich(String sessionId, TaskType taskType);
}
