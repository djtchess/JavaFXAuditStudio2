package ff.ss.javaFxAuditStudio.domain.workbench;

import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;
import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;

import java.util.List;
import java.util.Objects;

/**
 * Resultat immuable de l'orchestration bout-en-bout d'une session d'analyse.
 * Agrege les resultats de chaque etape du pipeline.
 *
 * @param sessionId         identifiant de la session orchestree
 * @param finalStatus       statut final apres orchestration (COMPLETED ou FAILED)
 * @param cartography       cartographie produite par l'etape cartographie (null si echec avant cette etape)
 * @param classification    resultat de classification (null si echec avant cette etape)
 * @param migrationPlan     plan de migration produit (null si echec avant cette etape)
 * @param generationResult  artefacts generes (null si echec avant cette etape)
 * @param restitutionReport rapport de restitution (null si echec avant cette etape)
 * @param errors            messages d'erreur ; vide si pipeline complete avec succes
 */
public record OrchestratedAnalysisResult(
        String sessionId,
        AnalysisStatus finalStatus,
        ControllerCartography cartography,
        ClassificationResult classification,
        MigrationPlan migrationPlan,
        GenerationResult generationResult,
        RestitutionReport restitutionReport,
        List<String> errors) {

    public OrchestratedAnalysisResult {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(finalStatus, "finalStatus must not be null");
        Objects.requireNonNull(errors, "errors must not be null");
        errors = List.copyOf(errors);
    }
}
