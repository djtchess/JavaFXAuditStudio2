package ff.ss.javaFxAuditStudio.domain.rules;

import java.util.List;
import java.util.Objects;

/**
 * Resultat de l'extraction des regles de gestion depuis un source Java.
 * Encapsule la liste des regles extraites et le mode de parsing utilise.
 * Si le mode est REGEX_FALLBACK, fallbackReason contient le message de l'exception JavaParser.
 *
 * @param rules          regles extraites, jamais null
 * @param parsingMode    mode d'extraction utilise, jamais null
 * @param fallbackReason raison du fallback regex, null si mode AST
 */
public record ExtractionResult(
        List<BusinessRule> rules,
        ParsingMode parsingMode,
        String fallbackReason) {

    public ExtractionResult {
        Objects.requireNonNull(rules, "rules must not be null");
        Objects.requireNonNull(parsingMode, "parsingMode must not be null");
        rules = List.copyOf(rules);
        // fallbackReason peut etre null si mode AST
    }

    /**
     * Construit un ExtractionResult en mode AST (parsing complet reussi).
     *
     * @param rules regles extraites par l'AST
     * @return resultat en mode AST
     */
    public static ExtractionResult ast(final List<BusinessRule> rules) {
        return new ExtractionResult(rules, ParsingMode.AST, null);
    }

    /**
     * Construit un ExtractionResult en mode fallback regex.
     *
     * @param rules  regles extraites par le fallback regex
     * @param reason raison de l'echec du parsing AST
     * @return resultat en mode REGEX_FALLBACK
     */
    public static ExtractionResult regexFallback(final List<BusinessRule> rules, final String reason) {
        return new ExtractionResult(rules, ParsingMode.REGEX_FALLBACK, reason);
    }
}
