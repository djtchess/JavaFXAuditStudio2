package ff.ss.javaFxAuditStudio.domain.rules;

import java.util.List;
import java.util.Objects;

/**
 * Résultat de la classification des responsabilités pour un controller donné.
 * Les règles certaines et incertaines sont conservées séparément pour permettre
 * au consommateur de traiter chaque catégorie selon sa politique de validation.
 *
 * @param controllerRef  référence du controller analysé
 * @param rules          règles classifiées avec certitude
 * @param uncertainRules règles dont la classification nécessite une validation humaine
 */
public record ClassificationResult(
        String controllerRef,
        List<BusinessRule> rules,
        List<BusinessRule> uncertainRules) {

    public ClassificationResult {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        Objects.requireNonNull(uncertainRules, "uncertainRules must not be null");
        rules = List.copyOf(rules);
        uncertainRules = List.copyOf(uncertainRules);
    }

    /**
     * Indique si le résultat contient des règles dont la classification est incertaine.
     *
     * @return vrai si au moins une règle incertaine est présente
     */
    public boolean hasUncertainties() {
        return !uncertainRules.isEmpty();
    }

    /**
     * Retourne la liste des règles (certaines et incertaines) correspondant
     * au candidat d'extraction demandé.
     *
     * @param type le type de candidat à filtrer
     * @return liste filtrée, jamais null
     */
    public List<BusinessRule> candidates(final ExtractionCandidate type) {
        Objects.requireNonNull(type, "type must not be null");
        return rules.stream()
                .filter(rule -> type.equals(rule.extractionCandidate()))
                .toList();
    }
}
