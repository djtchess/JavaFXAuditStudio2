package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.SanitizedSourcePreviewResponse;
import ff.ss.javaFxAuditStudio.application.ports.in.PreviewSanitizedSourceUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedSourcePreviewResult;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Controller REST pour la previsualisation du code sanitise (JAS-031).
 */
@Tag(name = "Sanitisation", description = "Previsualisation du code source desensibilise avant envoi au LLM")
@RestController
@RequestMapping("/api/v1/analyses")
public class SanitizedSourcePreviewController {

    private static final Logger LOG = LoggerFactory.getLogger(SanitizedSourcePreviewController.class);

    private final PreviewSanitizedSourceUseCase previewSanitizedSourceUseCase;

    public SanitizedSourcePreviewController(final PreviewSanitizedSourceUseCase previewSanitizedSourceUseCase) {
        this.previewSanitizedSourceUseCase = Objects.requireNonNull(previewSanitizedSourceUseCase);
    }

    @Operation(
            summary = "Previsualisation du code sanitise",
            description = "Retourne le code source desensibilise tel qu'il serait transmis au LLM, sans appel IA effectif.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Previsualisation generee avec succes"),
        @ApiResponse(responseCode = "404", description = "Session introuvable"),
        @ApiResponse(responseCode = "422", description = "Sanitisation refusee (marqueur sensible residuel ou depassement tokens)")
    })
    @PostMapping("/{sessionId}/preview-sanitized")
    public ResponseEntity<SanitizedSourcePreviewResponse> previewSanitized(
            @Parameter(description = "Identifiant de la session d'analyse", required = true)
            @PathVariable final String sessionId) {

        LOG.debug("Preview sanitise demande — session={}", sessionId);

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
        return new SanitizedSourcePreviewResponse(
                sessionId,
                bundle.controllerRef(),
                bundle.sanitizedSource(),
                bundle.estimatedTokens(),
                bundle.sanitizationVersion(),
                result.sanitized());
    }
}
