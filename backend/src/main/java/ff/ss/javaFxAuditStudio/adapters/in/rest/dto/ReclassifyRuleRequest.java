package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * Corps de la requete PATCH pour la reclassification d'une regle.
 *
 * @param category nom de la categorie de responsabilite cible (ex. "APPLICATION")
 * @param reason   raison fournie par l'utilisateur (texte libre, peut etre null)
 */
@Schema(description = "Requete de reclassification manuelle d'une regle metier")
public record ReclassifyRuleRequest(
        @Schema(description = "Nouvelle categorie : USE_CASE, GATEWAY, POLICY, VIEW_MODEL ou LIFECYCLE", example = "USE_CASE")
        String category,
        @Schema(description = "Justification de la reclassification", example = "La methode orchestre une action metier complexe")
        String reason) {

    public ReclassifyRuleRequest {
        Objects.requireNonNull(category, "category must not be null");
    }
}
