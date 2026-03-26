package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Artefacts de code Java generes")
public record ArtifactsResponse(
        @Schema(description = "Reference du controller source de la generation")
        String controllerRef,
        @Schema(description = "Liste des artefacts de code generes")
        List<CodeArtifactDto> artifacts,
        @Schema(description = "Avertissements globaux de la generation")
        List<String> warnings) {

    public ArtifactsResponse {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(artifacts, "artifacts must not be null");
        Objects.requireNonNull(warnings, "warnings must not be null");
        artifacts = List.copyOf(artifacts);
        warnings = List.copyOf(warnings);
    }

    /**
     * DTO d'un artefact genere, enrichi des avertissements de validation (JAS-009).
     */
    @Schema(description = "Artefact de code Java genere")
    public record CodeArtifactDto(
            @Schema(description = "Identifiant unique de l'artefact")
            String artifactId,
            @Schema(description = "Type d'artefact (ex: USE_CASE, GATEWAY, POLICY)")
            String type,
            @Schema(description = "Numero du lot auquel appartient cet artefact")
            int lotNumber,
            @Schema(description = "Nom qualifie simple de la classe generee")
            String className,
            @Schema(description = "Contenu source Java de l'artefact")
            String content,
            @Schema(description = "Vrai si l'artefact est un pont transitionnel temporaire")
            boolean transitionalBridge,
            @Schema(description = "Avertissements specifiques a la generation de cet artefact")
            List<String> generationWarnings,
            @Schema(description = "Statut de generation (OK, WARNING, ERROR)")
            String generationStatus) {

        public CodeArtifactDto {
            Objects.requireNonNull(artifactId, "artifactId must not be null");
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(className, "className must not be null");
            Objects.requireNonNull(content, "content must not be null");
            generationWarnings = (generationWarnings != null) ? List.copyOf(generationWarnings) : List.of();
            generationStatus = (generationStatus != null) ? generationStatus : "OK";
        }
    }
}
