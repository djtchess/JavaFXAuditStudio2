package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.Objects;

/**
 * Representation REST de l'etat complet d'une regle apres reclassification.
 *
 * @param ruleId              identifiant de la regle
 * @param description         enonce de la regle
 * @param responsibilityClass categorie de responsabilite apres reclassification
 * @param extractionCandidate candidat d'extraction recommande
 * @param uncertain           vrai si la classification necessite validation humaine
 * @param sourceRef           reference au fichier source
 * @param sourceLine          numero de ligne dans le fichier source
 */
public record ReclassifiedRuleResponse(
        String ruleId,
        String description,
        String responsibilityClass,
        String extractionCandidate,
        boolean uncertain,
        String sourceRef,
        int sourceLine) {

    public ReclassifiedRuleResponse {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(responsibilityClass, "responsibilityClass must not be null");
        Objects.requireNonNull(extractionCandidate, "extractionCandidate must not be null");
        Objects.requireNonNull(sourceRef, "sourceRef must not be null");
    }
}
