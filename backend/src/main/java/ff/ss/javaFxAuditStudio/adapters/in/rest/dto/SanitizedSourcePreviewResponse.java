package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * DTO de reponse pour la previsualisation du code sanitise (JAS-031 / QW-4).
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
        boolean sanitized,

        @Schema(description = "Nombre d'occurrences transformees par type de regle de sanitisation ; absent si pas de rapport")
        Map<String, Integer> transformationCountsByRuleType,

        @Schema(description = "Vrai si le rapport indique que la source est approuvee pour envoi au LLM ; absent si pas de rapport")
        Boolean reportApproved) {
}
