package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * DTO de reponse pour l'enrichissement IA (JAS-017).
 *
 * @param requestId         UUID de correlation
 * @param degraded          Vrai si mode degrade actif
 * @param degradationReason Raison du mode degrade, vide si nominal
 * @param suggestions       Cle = handlerName, valeur = suggestion
 * @param tokensUsed        Tokens consommes (0 en mode degrade)
 * @param provider          "claude-code" | "openai-gpt54" | "none"
 */
@Schema(description = "Resultat de l'enrichissement IA")
public record AiEnrichmentResponse(
        @Schema(description = "UUID de correlation de la requete d'enrichissement")
        String requestId,
        @Schema(description = "Vrai si le mode degrade est actif (pas de reponse IA disponible)")
        boolean degraded,
        @Schema(description = "Raison du mode degrade, chaine vide si mode nominal")
        String degradationReason,
        @Schema(description = "Suggestions IA : cle = nom du handler, valeur = suggestion textuelle")
        Map<String, String> suggestions,
        @Schema(description = "Tokens consommes lors de l'appel IA (0 en mode degrade)")
        int tokensUsed,
        @Schema(description = "Fournisseur IA utilise : claude-code, openai-gpt54 ou none")
        String provider) {
}
