package ff.ss.javaFxAuditStudio.domain.rules;

import java.util.Objects;

/**
 * Règle de gestion identifiée dans un controller JavaFX lors de l'analyse
 * de classification des responsabilités.
 *
 * @param ruleId                identifiant unique de la règle (ex. "RG-001")
 * @param description           énoncé de la règle telle qu'observée dans le source
 * @param sourceRef             référence au fichier source contenant la règle
 * @param sourceLine            numéro de ligne dans le fichier source (0 si inconnu)
 * @param responsibilityClass   classe de responsabilité à laquelle appartient la règle
 * @param extractionCandidate   candidat d'extraction recommandé pour cette règle
 * @param uncertain             vrai si la classification nécessite une validation humaine
 */
public record BusinessRule(
        String ruleId,
        String description,
        String sourceRef,
        int sourceLine,
        ResponsibilityClass responsibilityClass,
        ExtractionCandidate extractionCandidate,
        boolean uncertain) {

    public BusinessRule {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(sourceRef, "sourceRef must not be null");
        Objects.requireNonNull(responsibilityClass, "responsibilityClass must not be null");
        Objects.requireNonNull(extractionCandidate, "extractionCandidate must not be null");
    }
}
