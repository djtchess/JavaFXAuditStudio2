package ff.ss.javaFxAuditStudio.adapters.in.rest;

import java.util.Objects;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ArtifactCoherenceResponse;
import ff.ss.javaFxAuditStudio.application.ports.in.VerifyArtifactCoherenceUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.ArtifactCoherenceResult;
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

/**
 * Controller REST pour la verification de coherence inter-artefacts.
 */
@Tag(name = "Coherence IA")
@RestController
@RequestMapping("/api/v1/analyses")
public class ArtifactCoherenceController {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactCoherenceController.class);

    private final VerifyArtifactCoherenceUseCase verifyArtifactCoherenceUseCase;

    public ArtifactCoherenceController(final VerifyArtifactCoherenceUseCase verifyArtifactCoherenceUseCase) {
        this.verifyArtifactCoherenceUseCase = Objects.requireNonNull(verifyArtifactCoherenceUseCase);
    }

    @Operation(summary = "Verifier la coherence des artefacts",
            description = "Soumet la classification, la cartographie et les artefacts generes a un LLM pour verifier leur coherence.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verification effectuee (peut etre degradee)"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @PostMapping("/{sessionId}/coherence")
    public ResponseEntity<ArtifactCoherenceResponse> verify(
            @Parameter(description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId) {

        LOG.debug("Verification coherence IA demandee - session={}", sessionId);

        try {
            ArtifactCoherenceResult result = verifyArtifactCoherenceUseCase.verify(sessionId);
            return ResponseEntity.ok(toResponse(result));
        } catch (IllegalArgumentException ex) {
            LOG.debug("Session introuvable pour verification coherence IA : {}", sessionId);
            return ResponseEntity.notFound().build();
        }
    }

    private ArtifactCoherenceResponse toResponse(final ArtifactCoherenceResult result) {
        return new ArtifactCoherenceResponse(
                result.requestId(),
                result.degraded(),
                result.degradationReason(),
                result.coherent(),
                result.artifactIssues(),
                result.globalSuggestions(),
                result.provider().value());
    }
}
