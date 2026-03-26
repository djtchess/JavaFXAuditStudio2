package ff.ss.javaFxAuditStudio.application.ports.out;

import java.util.List;

import ff.ss.javaFxAuditStudio.domain.ai.LlmAuditEntry;

/**
 * Port sortant de persistance des entrees d'audit LLM (JAS-029).
 */
public interface LlmAuditPort {

    /**
     * Persiste une entree d'audit.
     *
     * @param entry entree d'audit a sauvegarder
     */
    void save(LlmAuditEntry entry);

    /**
     * Recherche toutes les entrees d'audit pour une session donnee.
     *
     * @param sessionId identifiant de session
     * @return liste des entrees (peut etre vide)
     */
    List<LlmAuditEntry> findBySessionId(String sessionId);
}
