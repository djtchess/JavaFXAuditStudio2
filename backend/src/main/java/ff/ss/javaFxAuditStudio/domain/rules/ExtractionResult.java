package ff.ss.javaFxAuditStudio.domain.rules;

import java.util.List;
import java.util.Objects;

/**
 * Resultat de l'extraction des regles de gestion depuis un source Java.
 * Encapsule la liste des regles extraites, le mode de parsing utilise et le
 * nombre de methodes lifecycle exclues de l'analyse.
 * Si le mode est REGEX_FALLBACK, fallbackReason contient le message de l'exception JavaParser.
 *
 * @param rules                        regles extraites, jamais null
 * @param parsingMode                  mode d'extraction utilise, jamais null
 * @param fallbackReason               raison du fallback regex, null si mode AST
 * @param excludedLifecycleMethodsCount nombre de methodes lifecycle ignorees, >= 0
 */
public record ExtractionResult(
        List<BusinessRule> rules,
        ParsingMode parsingMode,
        String fallbackReason,
        int excludedLifecycleMethodsCount) {

    public ExtractionResult {
        Objects.requireNonNull(rules, "rules must not be null");
        Objects.requireNonNull(parsingMode, "parsingMode must not be null");
        if (excludedLifecycleMethodsCount < 0) {
            throw new IllegalArgumentException("excludedLifecycleMethodsCount must be >= 0");
        }
        rules = List.copyOf(rules);
        // fallbackReason peut etre null si mode AST
    }

    /**
     * Construit un ExtractionResult en mode AST (parsing complet reussi), sans exclusion.
     *
     * @param rules regles extraites par l'AST
     * @return resultat en mode AST avec compteur d'exclusion a 0
     */
    public static ExtractionResult ast(final List<BusinessRule> rules) {
        return new ExtractionResult(rules, ParsingMode.AST, null, 0);
    }

    /**
     * Construit un ExtractionResult en mode AST avec compteur d'exclusion.
     *
     * @param rules         regles extraites par l'AST
     * @param excludedCount nombre de methodes lifecycle exclues
     * @return resultat en mode AST
     */
    public static ExtractionResult ast(final List<BusinessRule> rules, final int excludedCount) {
        return new ExtractionResult(rules, ParsingMode.AST, null, excludedCount);
    }

    /**
     * Construit un ExtractionResult en mode fallback regex, sans exclusion.
     *
     * @param rules  regles extraites par le fallback regex
     * @param reason raison de l'echec du parsing AST
     * @return resultat en mode REGEX_FALLBACK avec compteur d'exclusion a 0
     */
    public static ExtractionResult regexFallback(final List<BusinessRule> rules, final String reason) {
        return new ExtractionResult(rules, ParsingMode.REGEX_FALLBACK, reason, 0);
    }

    /**
     * Construit un ExtractionResult en mode fallback regex avec compteur d'exclusion.
     *
     * @param rules         regles extraites par le fallback regex
     * @param reason        raison de l'echec du parsing AST
     * @param excludedCount nombre de methodes lifecycle exclues
     * @return resultat en mode REGEX_FALLBACK
     */
    public static ExtractionResult regexFallback(final List<BusinessRule> rules, final String reason,
            final int excludedCount) {
        return new ExtractionResult(rules, ParsingMode.REGEX_FALLBACK, reason, excludedCount);
    }
}
