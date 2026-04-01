package ff.ss.javaFxAuditStudio.adapters.in.rest;

import java.util.Objects;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AiCodeGenerationResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ArtifactRefineRequest;
import ff.ss.javaFxAuditStudio.application.ports.in.RefineArtifactUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST pour le raffinement d'un artefact genere.
 */
@Tag(name = "Raffinement IA")
@RestController
@RequestMapping(path = {"/api/v1/analysis/sessions", "/api/v1/analyses"})
public class ArtifactRefinementController {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactRefinementController.class);

    private final RefineArtifactUseCase refineArtifactUseCase;

    public ArtifactRefinementController(final RefineArtifactUseCase refineArtifactUseCase) {
        this.refineArtifactUseCase = Objects.requireNonNull(refineArtifactUseCase);
    }

    @Operation(
            summary = "Raffiner un artefact genere",
            description = "Soumet un artefact existant, une consigne de raffinement et le contexte de la session a un LLM.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Raffinement effectue (peut etre degrade)"),
        @ApiResponse(responseCode = "400", description = "Requete invalide"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @PostMapping("/{sessionId}/refine")
    public ResponseEntity<AiCodeGenerationResponse> refine(
            @Parameter(description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId,
            @RequestBody final ArtifactRefineRequest request,
            @Parameter(description = "Fournisseur LLM cible (optionnel)", required = false)
            @RequestParam(required = false) final String provider) {

        LOG.debug("Raffinement IA demande - session={}, artifactType={}, provider={}",
                sessionId, request.artifactType(), provider);

        ArtifactType artifactType;
        try {
            artifactType = ArtifactType.valueOf(request.artifactType().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }

        LlmProvider parsedProvider = parseProvider(provider);
        if (provider != null && !provider.isBlank() && parsedProvider == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            ff.ss.javaFxAuditStudio.domain.ai.ArtifactRefineRequest domainRequest =
                    new ff.ss.javaFxAuditStudio.domain.ai.ArtifactRefineRequest(
                            artifactType,
                            request.instruction(),
                            request.previousCode());
            AiCodeGenerationResult result = (parsedProvider == null)
                    ? refineArtifactUseCase.refine(sessionId, domainRequest)
                    : refineArtifactUseCase.refine(sessionId, domainRequest, parsedProvider);
            return ResponseEntity.ok(toResponse(result));
        } catch (IllegalArgumentException ex) {
            LOG.debug("Session introuvable pour raffinement IA : {}", sessionId);
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

    private AiCodeGenerationResponse toResponse(final AiCodeGenerationResult result) {
        return new AiCodeGenerationResponse(
                result.requestId(),
                result.degraded(),
                result.degradationReason(),
                result.generatedClasses(),
                result.tokensUsed(),
                result.provider().value());
    }
}
