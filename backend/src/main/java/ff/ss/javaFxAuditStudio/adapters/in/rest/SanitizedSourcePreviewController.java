package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.SanitizationDryRunResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.SanitizedSourcePreviewResponse;
import ff.ss.javaFxAuditStudio.application.ports.in.PreviewSanitizedSourceUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedSourcePreviewResult;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Controller REST pour la previsualisation du code sanitise (JAS-031 / AI-2).
 *
 * <p>Le parametre optionnel {@code dryRun=true} active le mode dry-run :
 * les transformations sont collectees sans appel LLM et sans lever de
 * {@code SanitizationRefusedException}.
 */
@Tag(name = "Sanitisation", description = "Previsualisation du code source desensibilise avant envoi au LLM")
@RestController
@RequestMapping(path = {"/api/v1/analysis/sessions", "/api/v1/analyses"})
public class SanitizedSourcePreviewController {

    private static final Logger LOG = LoggerFactory.getLogger(SanitizedSourcePreviewController.class);

    private final PreviewSanitizedSourceUseCase previewSanitizedSourceUseCase;

    public SanitizedSourcePreviewController(final PreviewSanitizedSourceUseCase previewSanitizedSourceUseCase) {
        this.previewSanitizedSourceUseCase = Objects.requireNonNull(previewSanitizedSourceUseCase);
    }

    @Operation(
            summary = "Previsualisation du code sanitise",
            description = "Retourne le code source desensibilise tel qu'il serait transmis au LLM. "
                    + "Avec dryRun=true : retourne le rapport des transformations sans appel LLM "
                    + "et sans lever d'erreur de refus.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Previsualisation ou rapport dry-run genere avec succes"),
        @ApiResponse(responseCode = "404", description = "Session introuvable"),
        @ApiResponse(responseCode = "422", description = "Sanitisation refusee (mode normal uniquement)")
    })
    @PostMapping("/{sessionId}/preview-sanitized")
    public ResponseEntity<?> previewSanitized(
            @Parameter(description = "Identifiant de la session d'analyse", required = true)
            @PathVariable final String sessionId,
            @Parameter(description = "Active le mode dry-run : collecte les transformations sans refus ni appel LLM")
            @RequestParam(name = "dryRun", defaultValue = "false") final boolean dryRun) {

        LOG.debug("Preview sanitise demande — session={} dryRun={}", sessionId, dryRun);

        if (dryRun) {
            return handleDryRun(sessionId);
        }
        return handleNormal(sessionId);
    }

    private ResponseEntity<SanitizationDryRunResponse> handleDryRun(final String sessionId) {
        try {
            SanitizationReport report = previewSanitizedSourceUseCase.previewDryRun(sessionId);
            return ResponseEntity.ok(toDryRunResponse(sessionId, report));
        } catch (IllegalArgumentException ex) {
            LOG.debug("Session introuvable pour dry-run : {}", sessionId);
            return ResponseEntity.notFound().build();
        }
    }

    private ResponseEntity<SanitizedSourcePreviewResponse> handleNormal(final String sessionId) {
        try {
            SanitizedSourcePreviewResult result = previewSanitizedSourceUseCase.preview(sessionId);
            return ResponseEntity.ok(toResponse(sessionId, result));
        } catch (IllegalArgumentException ex) {
            LOG.debug("Session introuvable pour preview sanitise : {}", sessionId);
            return ResponseEntity.notFound().build();
        } catch (SanitizationRefusedException ex) {
            LOG.warn("Sanitisation refusee pour session {} : {}", sessionId, ex.getMessage());
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    private SanitizedSourcePreviewResponse toResponse(
            final String sessionId,
            final SanitizedSourcePreviewResult result) {
        SanitizedBundle bundle = result.bundle();
        SanitizationReport report = bundle.report();
        Map<String, Integer> countsByRuleType = buildCountsByRuleType(report);
        Boolean reportApproved = (report != null) ? report.approved() : null;
        return new SanitizedSourcePreviewResponse(
                sessionId,
                bundle.controllerRef(),
                bundle.sanitizedSource(),
                bundle.estimatedTokens(),
                bundle.sanitizationVersion(),
                result.sanitized(),
                countsByRuleType,
                reportApproved);
    }

    private Map<String, Integer> buildCountsByRuleType(final SanitizationReport report) {
        if (report == null) {
            return null;
        }
        return report.transformations().stream()
                .collect(Collectors.toMap(
                        t -> t.ruleType().name(),
                        t -> t.occurrenceCount(),
                        Integer::sum));
    }

    private SanitizationDryRunResponse toDryRunResponse(
            final String sessionId,
            final SanitizationReport report) {
        List<SanitizationDryRunResponse.TransformationEntry> entries = report.transformations().stream()
                .map(t -> new SanitizationDryRunResponse.TransformationEntry(
                        t.ruleType().name(),
                        t.occurrenceCount(),
                        t.description()))
                .toList();
        return new SanitizationDryRunResponse(
                sessionId,
                report.bundleId(),
                report.profileVersion(),
                report.sensitiveMarkersFound(),
                report.approved(),
                entries);
    }
}
