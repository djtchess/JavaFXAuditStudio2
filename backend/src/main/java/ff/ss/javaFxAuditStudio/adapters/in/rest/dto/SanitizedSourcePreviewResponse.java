package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de reponse pour la previsualisation du code sanitise (JAS-031).
 */
@Schema(description = "Previsualisation du code source apres passage dans le pipeline de sanitisation")
public record SanitizedSourcePreviewResponse(

        @Schema(description = "Identifiant de la session d'analyse")
        String sessionId,

        @Schema(description = "Reference au controller source (nom ou chemin)")
        String controllerRef,

        @Schema(description = "Code source desensibilise tel qu'il serait transmis au LLM")
        String sanitizedSource,

        @Schema(description = "Estimation du nombre de tokens avant envoi")
        int estimatedTokens,

        @Schema(description = "Version du pipeline de sanitisation applique")
        String sanitizationVersion,

        @Schema(description = "Vrai si la sanitisation a ete appliquee, faux si fallback brut")
        boolean sanitized) {
}
