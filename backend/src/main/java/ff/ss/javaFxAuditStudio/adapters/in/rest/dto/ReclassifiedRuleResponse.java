package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Regle apres reclassification")
public record ReclassifiedRuleResponse(
        @Schema(description = "Identifiant unique de la regle")
        String ruleId,
        @Schema(description = "Enonce de la regle metier")
        String description,
        @Schema(description = "Categorie de responsabilite apres reclassification")
        String responsibilityClass,
        @Schema(description = "Candidat d'extraction recommande")
        String extractionCandidate,
        @Schema(description = "Vrai si la classification necessite une validation humaine")
        boolean uncertain,
        @Schema(description = "Reference au fichier source contenant la regle")
        String sourceRef,
        @Schema(description = "Numero de ligne dans le fichier source")
        int sourceLine) {

    public ReclassifiedRuleResponse {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(responsibilityClass, "responsibilityClass must not be null");
        Objects.requireNonNull(extractionCandidate, "extractionCandidate must not be null");
        Objects.requireNonNull(sourceRef, "sourceRef must not be null");
    }
}
