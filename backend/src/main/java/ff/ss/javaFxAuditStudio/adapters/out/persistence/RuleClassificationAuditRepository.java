package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository Spring Data JPA pour les entrees d'audit de reclassification.
 */
public interface RuleClassificationAuditRepository
        extends JpaRepository<RuleClassificationAuditEntity, String> {

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
}
