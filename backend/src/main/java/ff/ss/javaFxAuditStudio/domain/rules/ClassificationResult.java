package ff.ss.javaFxAuditStudio.domain.rules;

import java.util.List;
import java.util.Objects;

/**
 * Resultat de la classification des responsabilites pour un controller donne.
 * Les regles certaines et incertaines sont conservees separement pour permettre
 * au consommateur de traiter chaque categorie selon sa politique de validation.
 *
 * @param controllerRef                reference du controller analyse
 * @param rules                        regles classifiees avec certitude
 * @param uncertainRules               regles dont la classification necessite une validation humaine
 * @param parsingMode                  mode de parsing utilise lors de l'extraction
 * @param parsingFallbackReason        raison du fallback regex, null si mode AST
 * @param excludedLifecycleMethodsCount nombre de methodes lifecycle ignorees lors de l'extraction
 */
public record ClassificationResult(
        String controllerRef,
        List<BusinessRule> rules,
        List<BusinessRule> uncertainRules,
        ParsingMode parsingMode,
        String parsingFallbackReason,
        int excludedLifecycleMethodsCount) {

    public ClassificationResult {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        Objects.requireNonNull(uncertainRules, "uncertainRules must not be null");
        Objects.requireNonNull(parsingMode, "parsingMode must not be null");
        if (excludedLifecycleMethodsCount < 0) {
            throw new IllegalArgumentException("excludedLifecycleMethodsCount must be >= 0");
        }
        rules = List.copyOf(rules);
        uncertainRules = List.copyOf(uncertainRules);
        // parsingFallbackReason peut etre null si mode AST
    }

    /**
     * Constructeur de compatibilite avec parsingMode et fallbackReason, sans compteur d'exclusion.
     *
     * @param controllerRef         reference du controller analyse
     * @param rules                 regles certaines
     * @param uncertainRules        regles incertaines
     * @param parsingMode           mode de parsing utilise
     * @param parsingFallbackReason raison du fallback regex
     */
    public ClassificationResult(
            final String controllerRef,
            final List<BusinessRule> rules,
            final List<BusinessRule> uncertainRules,
            final ParsingMode parsingMode,
            final String parsingFallbackReason) {
        this(controllerRef, rules, uncertainRules, parsingMode, parsingFallbackReason, 0);
    }

    /**
     * Constructeur de compatibilite pour les usages sans information de parsing (mode AST par defaut).
     *
     * @param controllerRef  reference du controller analyse
     * @param rules          regles certaines
     * @param uncertainRules regles incertaines
     */
    public ClassificationResult(
            final String controllerRef,
            final List<BusinessRule> rules,
            final List<BusinessRule> uncertainRules) {
        this(controllerRef, rules, uncertainRules, ParsingMode.AST, null, 0);
    }

    /**
     * Indique si le resultat contient des regles dont la classification est incertaine.
     *
     * @return vrai si au moins une regle incertaine est presente
     */
    public boolean hasUncertainties() {
        return !uncertainRules.isEmpty();
    }

    /**
     * Retourne la liste des regles (certaines et incertaines) correspondant
     * au candidat d'extraction demande.
     *
     * @param type le type de candidat a filtrer
     * @return liste filtree, jamais null
     */
    public List<BusinessRule> candidates(final ExtractionCandidate type) {
        Objects.requireNonNull(type, "type must not be null");
        return rules.stream()
                .filter(rule -> type.equals(rule.extractionCandidate()))
                .toList();
    }
}
