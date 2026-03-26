package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * DTO de reponse pour la revue IA des artefacts (JAS-030).
 */
@Schema(description = "Resultat de la revue IA des artefacts generes")
public record ArtifactReviewResponse(
        @Schema(description = "UUID de correlation") String requestId,
        @Schema(description = "Vrai si mode degrade") boolean degraded,
        @Schema(description = "Raison du mode degrade") String degradationReason,
        @Schema(description = "Score de qualite de migration 0-100, -1 si indisponible") int migrationScore,
        @Schema(description = "Revue par type d'artefact : USE_CASE, VIEW_MODEL, POLICY...") Map<String, String> artifactReviews,
        @Schema(description = "Suggestions de reclassification pour les regles incertaines") Map<String, String> uncertainReclassifications,
        @Schema(description = "Suggestions globales de migration") List<String> globalSuggestions,
        @Schema(description = "Fournisseur IA utilise") String provider) {
}
