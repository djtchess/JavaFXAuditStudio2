package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository Spring Data JPA pour les entrees d'audit de reclassification.
 */
public interface RuleClassificationAuditRepository
        extends JpaRepository<RuleClassificationAuditEntity, String> {

    List<RuleClassificationAuditEntity> findByAnalysisIdOrderByCreatedAtAsc(String analysisId);

    /**
     * Retourne toutes les entrees d'audit pour une analyse et une regle donnees,
     * triees par date de creation ascendante.
     *
     * @param analysisId identifiant de la session d'analyse
     * @param ruleId     identifiant de la regle
     * @return liste triee, jamais null
     */
    List<RuleClassificationAuditEntity> findByAnalysisIdAndRuleIdOrderByCreatedAtAsc(
            String analysisId, String ruleId);

    /**
     * Compte le nombre total de reclassifications manuelles pour un projet.
     *
     * @param projectId identifiant du projet (= controllerName des sessions)
     * @return nombre de reclassifications
     */
    @Query("""
            SELECT COUNT(a)
            FROM RuleClassificationAuditEntity a
            WHERE a.analysisId IN (
                SELECT s.sessionId FROM AnalysisSessionEntity s WHERE s.controllerName = :projectId
            )
            """)
    long countReclassificationsForProject(@Param("projectId") String projectId);
}
