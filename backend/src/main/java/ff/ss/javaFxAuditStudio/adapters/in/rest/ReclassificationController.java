package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ReclassificationAuditEntryResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ReclassifiedRuleResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ReclassifyRuleRequest;
import ff.ss.javaFxAuditStudio.application.ports.in.GetReclassificationHistoryUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ReclassifyRuleUseCase;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ReclassificationAuditEntry;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Controller REST pour la reclassification manuelle des regles de gestion.
 * Expose les endpoints JAS-013 :
 * - PATCH /api/v1/analysis/sessions/{analysisId}/rules/{ruleId}/classification
 * - GET  /api/v1/analysis/sessions/{analysisId}/rules/{ruleId}/classification/history
 *
 * <p>Compatibilite legacy maintenue temporairement sur `/api/v1/analyses/...`.
 */
@Tag(name = "Reclassification")
@RestController
@RequestMapping(path = {"/api/v1/analysis/sessions", "/api/v1/analyses"})
public class ReclassificationController {

    private static final Logger log = LoggerFactory.getLogger(ReclassificationController.class);

    private final ReclassifyRuleUseCase reclassifyRuleUseCase;
    private final GetReclassificationHistoryUseCase getReclassificationHistoryUseCase;

    public ReclassificationController(
            final ReclassifyRuleUseCase reclassifyRuleUseCase,
            final GetReclassificationHistoryUseCase getReclassificationHistoryUseCase) {
        this.reclassifyRuleUseCase = Objects.requireNonNull(reclassifyRuleUseCase);
        this.getReclassificationHistoryUseCase = Objects.requireNonNull(getReclassificationHistoryUseCase);
    }

    /**
     * Reclassifie manuellement une regle de gestion dans une session d'analyse.
     *
     * <ul>
     *   <li>200 OK avec l'etat complet de la regle apres modification.</li>
     *   <li>400 Bad Request si la categorie est invalide.</li>
     *   <li>404 Not Found si la session ou la regle est introuvable.</li>
     *   <li>409 Conflict si la session est en statut LOCKED.</li>
     * </ul>
     */
    @Operation(summary = "Reclassifier une regle", description = "Modifie la categorie d'une regle metier avec justification. Enregistre l'action dans l'audit trail.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Regle reclassifiee"),
        @ApiResponse(responseCode = "400", description = "Categorie invalide"),
        @ApiResponse(responseCode = "404", description = "Session ou regle introuvable")
    })
    @PatchMapping("/{analysisId}/rules/{ruleId}/classification")
    public ResponseEntity<ReclassifiedRuleResponse> reclassify(
            @Parameter(name = "analysisId", description = "Identifiant de la session d'analyse", required = true)
            @PathVariable final String analysisId,
            @Parameter(name = "ruleId", description = "Identifiant de la regle metier", required = true)
            @PathVariable final String ruleId,
            @RequestBody final ReclassifyRuleRequest request) {

        ResponsibilityClass newCategory = parseCategory(request.category());
        if (newCategory == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            BusinessRule updated = reclassifyRuleUseCase.reclassify(
                    analysisId, ruleId, newCategory, request.reason());
            return ResponseEntity.ok(toResponse(updated));
        } catch (NoSuchElementException ex) {
            log.debug("Reclassification echouee - element introuvable : {}", ex.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException ex) {
            log.debug("Reclassification refusee - session verrouilee : {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * Retourne l'historique de reclassification pour une regle dans une analyse.
     *
     * <ul>
     *   <li>200 OK avec la liste des entrees d'audit (peut etre vide).</li>
     * </ul>
     */
    @Operation(summary = "Historique de reclassification", description = "Retourne l'historique complet des reclassifications pour une regle donnee.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historique disponible"),
        @ApiResponse(responseCode = "404", description = "Session ou regle introuvable")
    })
    @GetMapping("/{analysisId}/rules/{ruleId}/classification/history")
    public List<ReclassificationAuditEntryResponse> getHistory(
            @Parameter(name = "analysisId", description = "Identifiant de la session d'analyse", required = true)
            @PathVariable final String analysisId,
            @Parameter(name = "ruleId", description = "Identifiant de la regle metier", required = true)
            @PathVariable final String ruleId) {
        return getReclassificationHistoryUseCase
                .handle(analysisId, ruleId)
                .stream()
                .map(this::toAuditResponse)
                .toList();
    }

    private ResponsibilityClass parseCategory(final String category) {
        try {
            return ResponsibilityClass.valueOf(category);
        } catch (IllegalArgumentException ex) {
            log.debug("Categorie invalide fournie : {}", category);
            return null;
        }
    }

    private ReclassifiedRuleResponse toResponse(final BusinessRule rule) {
        return new ReclassifiedRuleResponse(
                rule.ruleId(),
                rule.description(),
                rule.responsibilityClass().name(),
                rule.extractionCandidate().name(),
                rule.uncertain(),
                rule.sourceRef(),
                rule.sourceLine());
    }

    private ReclassificationAuditEntryResponse toAuditResponse(final ReclassificationAuditEntry entry) {
        return new ReclassificationAuditEntryResponse(
                entry.ruleId(),
                entry.fromCategory().name(),
                entry.toCategory().name(),
                entry.reason(),
                entry.timestamp());
    }
}
