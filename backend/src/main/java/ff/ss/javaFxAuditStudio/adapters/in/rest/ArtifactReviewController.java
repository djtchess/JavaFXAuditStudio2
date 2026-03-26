package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ArtifactReviewResponse;
import ff.ss.javaFxAuditStudio.application.ports.in.ReviewArtifactsUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.ArtifactReviewResult;
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

    @Operation(summary = "Revue IA des artefacts",
            description = "Soumet les regles classifiees de la session a un LLM pour obtenir une revue de qualite de migration.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Revue effectuee (peut etre degradee)"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @PostMapping("/{sessionId}/review")
    public ResponseEntity<ArtifactReviewResponse> review(
            @Parameter(description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId) {

        LOG.debug("Revue IA demandee — session={}", sessionId);

        try {
            ArtifactReviewResult result = reviewArtifactsUseCase.review(sessionId);
            return ResponseEntity.ok(toResponse(result));
        } catch (IllegalArgumentException ex) {
            LOG.debug("Session introuvable pour revue IA : {}", sessionId);
            return ResponseEntity.notFound().build();
        }
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
