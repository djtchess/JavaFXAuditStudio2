package ff.ss.javaFxAuditStudio.adapters.in.rest;

import java.util.Objects;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AiEnrichmentResponse;
import ff.ss.javaFxAuditStudio.application.ports.in.EnrichAnalysisUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
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
 * Controller REST pour l'enrichissement IA d'une session d'analyse (JAS-017).
 */
@Tag(name = "Enrichissement IA")
@RestController
@RequestMapping("/api/v1/analyses")
public class AiEnrichmentController {

    private static final Logger LOG = LoggerFactory.getLogger(AiEnrichmentController.class);
    private static final String DEFAULT_TASK_TYPE = "NAMING";

    private final EnrichAnalysisUseCase enrichAnalysisUseCase;

    public AiEnrichmentController(final EnrichAnalysisUseCase enrichAnalysisUseCase) {
        this.enrichAnalysisUseCase = Objects.requireNonNull(enrichAnalysisUseCase);
    }

    @Operation(
            summary = "Enrichissement IA",
            description = "Soumet les regles de la session a un LLM pour obtenir des suggestions de nommage.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Enrichissement effectue (peut etre degrade)"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @PostMapping("/{sessionId}/enrich")
    public ResponseEntity<AiEnrichmentResponse> enrich(
            @Parameter(name = "sessionId", description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId,
            @Parameter(name = "taskType", description = "Type de tache IA (NAMING, DESCRIPTION)", required = false)
            @RequestParam(defaultValue = DEFAULT_TASK_TYPE) final String taskType,
            @Parameter(name = "provider", description = "Fournisseur LLM cible (optionnel)", required = false)
            @RequestParam(required = false) final String provider) {

        LOG.debug("Enrichissement demande - session={}, taskType={}, provider={}", sessionId, taskType, provider);

        TaskType parsedTaskType;
        try {
            parsedTaskType = TaskType.valueOf(taskType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            LOG.debug("TaskType invalide : {}", taskType);
            return ResponseEntity.badRequest().build();
        }

        LlmProvider parsedProvider = parseProvider(provider);
        if (provider != null && !provider.isBlank() && parsedProvider == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            AiEnrichmentResult result = (parsedProvider == null)
                    ? enrichAnalysisUseCase.enrich(sessionId, parsedTaskType)
                    : enrichAnalysisUseCase.enrich(sessionId, parsedTaskType, parsedProvider);
            return ResponseEntity.ok(toResponse(result));
        } catch (IllegalArgumentException ex) {
            LOG.debug("Session introuvable pour enrichissement : {}", sessionId);
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

    private AiEnrichmentResponse toResponse(final AiEnrichmentResult result) {
        return new AiEnrichmentResponse(
                result.requestId(),
                result.degraded(),
                result.degradationReason(),
                result.suggestions(),
                result.tokensUsed(),
                result.provider().value());
    }
}
