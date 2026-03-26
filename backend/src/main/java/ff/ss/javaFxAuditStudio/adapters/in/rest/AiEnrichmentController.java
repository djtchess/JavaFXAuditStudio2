package ff.ss.javaFxAuditStudio.adapters.in.rest;

import java.util.Objects;

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

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AiEnrichmentResponse;
import ff.ss.javaFxAuditStudio.application.ports.in.EnrichAnalysisUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;

/**
 * Controller REST pour l'enrichissement IA d'une session d'analyse (JAS-017).
 *
 * <p>Expose :
 * <ul>
 *   <li>{@code POST /api/v1/analyses/{sessionId}/enrich?taskType=NAMING}</li>
 * </ul>
 *
 * <p>Codes de retour :
 * <ul>
 *   <li>200 OK — enrichissement nominal ou degrade (le champ {@code degraded} le distingue)</li>
 *   <li>404 Not Found — session introuvable</li>
 * </ul>
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

    /**
     * Enrichit la session d'analyse avec le fournisseur IA configure.
     *
     * @param sessionId identifiant de la session
     * @param taskType  type de tache : NAMING | DESCRIPTION | CLASSIFICATION_HINT (defaut : NAMING)
     * @return 200 avec le resultat (nominal ou degrade), 404 si session introuvable
     */
    @Operation(summary = "Enrichissement IA", description = "Soumet les regles de la session a un LLM (Claude ou OpenAI) pour obtenir des suggestions de nommage. Retourne un resultat degrade si le LLM est indisponible.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Enrichissement effectue (peut etre degrade)"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @PostMapping("/{sessionId}/enrich")
    public ResponseEntity<AiEnrichmentResponse> enrich(
            @Parameter(name = "sessionId", description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId,
            @Parameter(name = "taskType", description = "Type de tache IA (NAMING, DESCRIPTION)", required = false)
            @RequestParam(defaultValue = DEFAULT_TASK_TYPE) final String taskType) {

        LOG.debug("Enrichissement demande — session={}, taskType={}", sessionId, taskType);

        TaskType parsedTaskType;
        try {
            parsedTaskType = TaskType.valueOf(taskType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            LOG.debug("TaskType invalide : {}", taskType);
            return ResponseEntity.badRequest().build();
        }

        try {
            AiEnrichmentResult result = enrichAnalysisUseCase.enrich(sessionId, parsedTaskType);
            return ResponseEntity.ok(toResponse(result));
        } catch (IllegalArgumentException ex) {
            LOG.debug("Session introuvable pour enrichissement : {}", sessionId);
            return ResponseEntity.notFound().build();
        }
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
