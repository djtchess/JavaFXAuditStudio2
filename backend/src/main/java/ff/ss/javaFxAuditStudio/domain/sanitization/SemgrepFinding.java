package ff.ss.javaFxAuditStudio.domain.sanitization;

/**
 * Represente un finding retourne par un scan Semgrep (JAS-018).
 *
 * <p>Objet immuable du domaine. Ne contient jamais le contenu complet du source
 * pour eviter toute fuite de donnee sensible dans les logs.
 *
 * @param ruleId   Identifiant de la regle Semgrep ayant produit ce finding
 * @param line     Numero de ligne dans le source temporaire
 * @param severity Severite du finding : ERROR, WARNING ou INFO
 * @param message  Message descriptif de la regle (sans donnee sensible)
 * @param snippet  Extrait de code concerne (peut etre vide)
 */
public record SemgrepFinding(
        String ruleId,
        int line,
        String severity,
        String message,
        String snippet) {
}
