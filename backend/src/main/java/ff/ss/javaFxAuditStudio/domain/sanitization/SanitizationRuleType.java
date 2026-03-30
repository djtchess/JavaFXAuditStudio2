package ff.ss.javaFxAuditStudio.domain.sanitization;

/**
 * Types de regles de sanitisation appliquees avant tout appel LLM (JAS-018).
 *
 * <p>Chaque valeur correspond a une transformation distincte
 * dans le pipeline {@code SanitizationPipelineAdapter}.
 */
public enum SanitizationRuleType {
    /** Noms metier (classes, methodes, champs) remplaces par des termes generiques. */
    IDENTIFIER_REPLACEMENT,
    /** Secrets, URLs et mots de passe remplaces par des placeholders. */
    SECRET_REMOVAL,
    /** Commentaires sensibles supprimes ou neutralises. */
    COMMENT_REMOVAL,
    /** Donnees reelles (emails, numeros, chaines) remplacees par des donnees fictives. */
    DATA_SUBSTITUTION,
    /** Scan Semgrep post-sanitisation : detecte les findings de securite residuels sans modifier le source. */
    SEMGREP_SECURITY_SCAN,
    /**
     * Remise en forme AST via OpenRewrite : renomme les declarations de classe contenant
     * un suffixe metier. Fonctionne en mode best-effort ; bascule sur regex si le
     * classpath complet n'est pas disponible (cas courant pour du code isole).
     */
    OPENREWRITE_REMEDIATION,
    /**
     * Audit pré-sanitisation : analyse le source brut et comptabilise les elements sensibles
     * detectes (classes metier, secrets, URLs, emails, commentaires, annotations JPA).
     * N'applique aucune transformation — le source est retourne inchange.
     * Positionne en tete du pipeline pour etablir une base de reference avant toute modification.
     */
    PRE_SANITIZATION_AUDIT
}
