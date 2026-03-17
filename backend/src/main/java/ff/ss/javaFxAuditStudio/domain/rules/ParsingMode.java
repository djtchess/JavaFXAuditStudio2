package ff.ss.javaFxAuditStudio.domain.rules;

/**
 * Mode d'extraction des regles de gestion depuis le source Java.
 * AST signifie que JavaParser a reussi a produire un arbre syntaxique complet.
 * REGEX_FALLBACK signifie que JavaParser a echoue et que l'analyse textuelle par regex a pris le relais.
 */
public enum ParsingMode {
    AST,
    REGEX_FALLBACK
}
