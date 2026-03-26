package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO de reponse pour le mode dry-run de la sanitisation (AI-2).
 *
 * <p>Expose le rapport de transformations collectees sans appel LLM
 * et sans lever de SanitizationRefusedException.
 */
@Schema(description = "Rapport dry-run du pipeline de sanitisation : transformations collectees sans appel LLM")
public record SanitizationDryRunResponse(

        @Schema(description = "Identifiant de la session d'analyse")
        String sessionId,

        @Schema(description = "Identifiant du bundle utilise pour ce dry-run")
        String bundleId,

        @Schema(description = "Version du profil de sanitisation applique")
        String profileVersion,

        @Schema(description = "Vrai si des marqueurs sensibles ont ete detectes apres passage dans le pipeline")
        boolean sensitiveMarkersFound,

        @Schema(description = "Vrai si le rapport indique que la source pourrait etre transmise au LLM")
        boolean approved,

        @Schema(description = "Transformations collectees par chaque sanitizer")
        List<TransformationEntry> transformations) {

    /**
     * Detail d'une transformation collectee lors du dry-run.
     */
    @Schema(description = "Detail d'une transformation collectee lors du dry-run")
    public record TransformationEntry(

            @Schema(description = "Type de regle appliquee")
            String ruleType,

            @Schema(description = "Nombre d'occurrences remplacees ou supprimees")
            int occurrenceCount,

            @Schema(description = "Description courte de la transformation")
            String description) {
    }
}
