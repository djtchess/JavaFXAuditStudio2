package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reponse de l'endpoint GET /api/v1/ai-enrichment/status (JAS-022).
 *
 * <p>Regles de securite :
 * - La valeur de l'API key n'est JAMAIS exposee.
 * - Seul {@code credentialPresent} indique la presence (true/false) d'un credential valide.
 */
@Schema(description = "Statut du service d'enrichissement IA")
public record AiEnrichmentStatusResponse(
        @Schema(description = "Vrai si le service d'enrichissement IA est active")
        boolean enabled,
        @Schema(description = "Fournisseur IA configure (ex: claude-code, openai-gpt54, claude-code-cli, openai-codex-cli, none)")
        String provider,
        @Schema(description = "Vrai si un credential valide est present (la valeur n'est jamais exposee)")
        boolean credentialPresent,
        @Schema(description = "Delai maximal d'appel au service IA en millisecondes")
        long timeoutMs) {}
