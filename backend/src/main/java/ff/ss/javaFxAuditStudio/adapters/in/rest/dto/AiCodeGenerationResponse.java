package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * DTO de reponse pour la generation IA des classes cibles Spring Boot (JAS-031).
 *
 * @param requestId         UUID de correlation
 * @param degraded          Vrai si le mode degrade est actif
 * @param degradationReason Raison du mode degrade, chaine vide si nominal
 * @param generatedClasses  Cle = type d'artefact (USE_CASE, VIEW_MODEL, POLICY, GATEWAY),
 *                          valeur = code Java complet genere par le LLM
 * @param tokensUsed        Tokens consommes (0 en mode degrade)
 * @param provider          Fournisseur IA utilise : "claude-code" | "openai-gpt54" | "claude-code-cli" | "openai-codex-cli" | "none"
 */
@Schema(description = "Resultat de la generation IA des classes Spring Boot cibles")
public record AiCodeGenerationResponse(
        @Schema(description = "UUID de correlation de la requete de generation")
        String requestId,
        @Schema(description = "Vrai si le mode degrade est actif (pas de generation disponible)")
        boolean degraded,
        @Schema(description = "Raison du mode degrade, chaine vide si mode nominal")
        String degradationReason,
        @Schema(description = "Classes generees : cle = type d'artefact (USE_CASE, VIEW_MODEL, POLICY, GATEWAY), valeur = code Java complet")
        Map<String, String> generatedClasses,
        @Schema(description = "Tokens consommes lors de l'appel IA (0 en mode degrade)")
        int tokensUsed,
        @Schema(description = "Fournisseur IA utilise : claude-code, openai-gpt54, claude-code-cli, openai-codex-cli ou none")
        String provider) {
}
