package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.rules.ReclassificationAuditEntry;

import java.util.List;

/**
 * Port sortant pour la persistence des entrees d'audit de reclassification.
 */
public interface ReclassificationAuditPort {

    /**
     * Persiste une entree d'audit de reclassification.
     *
     * @param entry l'entree a persister
     * @return l'entree persistee (avec l'identifiant genere si applicable)
     */
    ReclassificationAuditEntry save(ReclassificationAuditEntry entry);

    /**
     * Retourne l'historique de reclassification pour une regle dans une analyse donnee.
     *
     * @param analysisId identifiant de la session d'analyse
     * @param ruleId     identifiant de la regle
     * @return liste des entrees d'audit triees par horodatage ascendant, jamais null
     */
    List<ReclassificationAuditEntry> findByAnalysisIdAndRuleId(String analysisId, String ruleId);
}
