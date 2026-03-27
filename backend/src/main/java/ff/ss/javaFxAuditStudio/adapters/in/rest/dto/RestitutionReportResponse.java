package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Rapport synthetique de l'analyse")
public record RestitutionReportResponse(
        @Schema(description = "Reference du controller analyse")
        String controllerRef,
        @Schema(description = "Nombre de regles metier extraites")
        int ruleCount,
        @Schema(description = "Nombre d'artefacts generes")
        int artifactCount,
        @Schema(description = "Niveau de confiance global de l'analyse (ex: HIGH, MEDIUM, LOW)")
        String confidence,
        @Schema(description = "Vrai si le rapport est suffisamment complet pour lancer une migration")
        boolean isActionable,
        @Schema(description = "Liste des observations et recommandations issues de l'analyse")
        List<String> findings,
        @Schema(description = "Liste des elements non resolus necessitant une action manuelle")
        List<String> unknowns,
        @Schema(description = "Version markdown complete de la restitution")
        String markdown) {

    public RestitutionReportResponse {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(confidence, "confidence must not be null");
        Objects.requireNonNull(findings, "findings must not be null");
        Objects.requireNonNull(unknowns, "unknowns must not be null");
        findings = List.copyOf(findings);
        unknowns = List.copyOf(unknowns);
        markdown = markdown == null ? "" : markdown;
    }
}
