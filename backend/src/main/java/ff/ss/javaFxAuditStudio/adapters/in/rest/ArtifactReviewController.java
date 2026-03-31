package ff.ss.javaFxAuditStudio.adapters.in.rest;

import java.util.Objects;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ArtifactReviewResponse;
import ff.ss.javaFxAuditStudio.application.ports.in.ReviewArtifactsUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.ArtifactReviewResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
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

/**
 * Controller REST pour la revue IA des artefacts generes (JAS-030).
 */
@Tag(name = "Revue IA")
@RestController
@RequestMapping("/api/v1/analyses")
public class ArtifactReviewController {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactReviewController.class);

    private final ReviewArtifactsUseCase reviewArtifactsUseCase;

    public ArtifactReviewController(final ReviewArtifactsUseCase reviewArtifactsUseCase) {
        this.reviewArtifactsUseCase = Objects.requireNonNull(reviewArtifactsUseCase);
    }

    @Operation(
            summary = "Revue IA des artefacts",
            description = "Soumet les regles classifiees de la session a un LLM pour obtenir une revue de qualite.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Revue effectuee (peut etre degradee)"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @PostMapping("/{sessionId}/review")
    public ResponseEntity<ArtifactReviewResponse> review(
            @Parameter(description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId,
            @Parameter(description = "Fournisseur LLM cible (optionnel)", required = false)
            @RequestParam(required = false) final String provider) {

        LOG.debug("Revue IA demandee - session={}, provider={}", sessionId, provider);
        LlmProvider parsedProvider = parseProvider(provider);
        if (provider != null && !provider.isBlank() && parsedProvider == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            ArtifactReviewResult result = (parsedProvider == null)
                    ? reviewArtifactsUseCase.review(sessionId)
                    : reviewArtifactsUseCase.review(sessionId, parsedProvider);
            return ResponseEntity.ok(toResponse(result));
        } catch (IllegalArgumentException ex) {
            LOG.debug("Session introuvable pour revue IA : {}", sessionId);
            return ResponseEntity.notFound().build();
        }
    }

    private LlmProvider parseProvider(final String provider) {
        if (provider == null || provider.isBlank()) {
            return null;
        }
        LlmProvider parsed = LlmProvider.fromString(provider);
        if (parsed == LlmProvider.NONE && !"none".equalsIgnoreCase(provider.trim())) {
            return null;
        }
        if (parsed == LlmProvider.NONE) {
            return null;
        }
        return parsed;
    }

    private ArtifactReviewResponse toResponse(final ArtifactReviewResult result) {
        return new ArtifactReviewResponse(
                result.requestId(),
                result.degraded(),
                result.degradationReason(),
                result.migrationScore(),
                result.artifactReviews(),
                result.uncertainReclassifications(),
                result.globalSuggestions(),
                result.provider().value());
    }
}
