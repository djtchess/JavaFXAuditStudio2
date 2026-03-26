package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository Spring Data JPA pour les entrees d'audit LLM (JAS-029).
 */
public interface LlmAuditRepository extends JpaRepository<LlmAuditEntity, String> {

    /**
     * Recherche toutes les entrees d'audit pour une session donnee.
     *
     * @param sessionId identifiant de session
     * @return liste des entrees
     */
    List<LlmAuditEntity> findBySessionId(String sessionId);
}
