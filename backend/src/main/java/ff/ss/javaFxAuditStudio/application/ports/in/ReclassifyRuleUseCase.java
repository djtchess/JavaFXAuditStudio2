package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;

/**
 * Port entrant pour la reclassification manuelle d'une regle de gestion.
 */
public interface ReclassifyRuleUseCase {

    /**
     * Reclassifie une regle dans la session d'analyse indiquee.
     *
     * <p>Preconditions :
     * <ul>
     *   <li>La session doit exister (lance {@link java.util.NoSuchElementException} sinon).</li>
     *   <li>La regle doit exister dans la session (lance {@link java.util.NoSuchElementException} sinon).</li>
     *   <li>La session ne doit pas etre en statut {@code LOCKED} (lance {@link IllegalStateException} sinon).</li>
     * </ul>
     *
     * @param analysisId  identifiant de la session d'analyse
     * @param ruleId      identifiant de la regle a reclassifier
     * @param newCategory nouvelle categorie de responsabilite
     * @param reason      raison fournie par l'utilisateur (peut etre null)
     * @return la regle apres modification
     */
    BusinessRule reclassify(String analysisId, String ruleId, ResponsibilityClass newCategory, String reason);
}
