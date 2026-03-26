package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationTransformation;

/**
 * Contrat interne du pipeline de sanitisation (JAS-018).
 *
 * <p>Chaque implementeur applique une regle specifique sur la source.
 * La methode {@link #apply(String)} doit etre appelee avant {@link #report()}.
 * Les implementeurs ne sont pas thread-safe — creer une instance par appel.
 */
public interface Sanitizer {

    /**
     * Applique la regle de sanitisation sur la source et retourne la source transformee.
     *
     * @param source source brute ou partiellement sanitisee
     * @return source apres application de la regle
     */
    String apply(String source);

    /**
     * Retourne les statistiques de la derniere transformation effectuee.
     * Doit etre appele apres {@link #apply(String)}.
     *
     * @return transformation avec compteur d'occurrences
     */
    SanitizationTransformation report();

    /**
     * Retourne le type de regle que ce sanitizer applique.
     *
     * @return type de regle
     */
    SanitizationRuleType ruleType();
}
