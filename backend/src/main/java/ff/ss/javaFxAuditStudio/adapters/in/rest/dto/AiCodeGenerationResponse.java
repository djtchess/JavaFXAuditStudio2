package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * DTO de réponse pour la génération IA des classes cibles Spring Boot (JAS-031).
 *
 * @param requestId          UUID de corrélation
 * @param degraded           Vrai si le mode dégradé est actif
 * @param degradationReason  Raison du mode dégradé, chaîne vide si nominal
 * @param generatedClasses   Clé = type d'artefact (USE_CASE, VIEW_MODEL, POLICY, GATEWAY),
 *                           valeur = code Java complet généré par le LLM
 * @param tokensUsed         Tokens consommés (0 en mode dégradé)
 * @param provider           Fournisseur IA utilisé : "claude-code" | "openai-gpt54" | "none"
 */
@Schema(description = "Résultat de la génération IA des classes Spring Boot cibles")
public record AiCodeGenerationResponse(
        @Schema(description = "UUID de corrélation de la requête de génération")
        String requestId,
        @Schema(description = "Vrai si le mode dégradé est actif (pas de génération disponible)")
        boolean degraded,
        @Schema(description = "Raison du mode dégradé, chaîne vide si mode nominal")
        String degradationReason,
        @Schema(description = "Classes générées : clé = type d'artefact (USE_CASE, VIEW_MODEL, POLICY, GATEWAY), valeur = code Java complet")
        Map<String, String> generatedClasses,
        @Schema(description = "Tokens consommés lors de l'appel IA (0 en mode dégradé)")
        int tokensUsed,
        @Schema(description = "Fournisseur IA utilisé : claude-code, openai-gpt54 ou none")
        String provider) {
}
