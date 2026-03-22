package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.Objects;

/**
 * Corps de la requete PATCH pour la reclassification d'une regle.
 *
 * @param category nom de la categorie de responsabilite cible (ex. "APPLICATION")
 * @param reason   raison fournie par l'utilisateur (texte libre, peut etre null)
 */
public record ReclassifyRuleRequest(String category, String reason) {

    public ReclassifyRuleRequest {
        Objects.requireNonNull(category, "category must not be null");
    }
}
