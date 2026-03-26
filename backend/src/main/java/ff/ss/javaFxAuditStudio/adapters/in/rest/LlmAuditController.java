package ff.ss.javaFxAuditStudio.adapters.in.rest;

import java.util.List;
import java.util.Objects;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.LlmAuditEntryResponse;
import ff.ss.javaFxAuditStudio.application.ports.out.LlmAuditPort;
import ff.ss.javaFxAuditStudio.domain.ai.LlmAuditEntry;

/**
 * Controller REST pour la consultation de l'audit des appels LLM sanitises (JAS-029).
 *
 * <p>Expose :
 * <ul>
 *   <li>{@code GET /api/v1/analysis/sessions/{sessionId}/llm-audit}</li>
 * </ul>
 *
 * <p>Codes de retour :
 * <ul>
 *   <li>200 OK — liste des entrees d'audit (peut etre vide)</li>
 * </ul>
 */
@Tag(name = "Audit LLM")
@RestController
@RequestMapping("/api/v1/analysis/sessions")
public class LlmAuditController {

    private static final Logger LOG = LoggerFactory.getLogger(LlmAuditController.class);

    private final LlmAuditPort llmAuditPort;

    public LlmAuditController(final LlmAuditPort llmAuditPort) {
        this.llmAuditPort = Objects.requireNonNull(llmAuditPort, "llmAuditPort must not be null");
    }

    /**
     * Retourne l'historique des appels LLM sanitises pour une session.
     *
     * @param sessionId identifiant de la session d'analyse
     * @return liste des entrees d'audit (peut etre vide)
     */
    @Operation(summary = "Journal d'audit LLM", description = "Retourne l'historique des appels LLM pour une session : provider, hash du payload, tokens utilises, mode degrade.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Audit disponible"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @GetMapping("/{sessionId}/llm-audit")
    public List<LlmAuditEntryResponse> getAuditEntries(
            @Parameter(name = "sessionId", description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId) {
        LOG.debug("Consultation audit LLM — session={}", sessionId);
        return llmAuditPort.findBySessionId(sessionId)
                .stream()
                .map(LlmAuditController::toResponse)
                .toList();
    }

    private static LlmAuditEntryResponse toResponse(final LlmAuditEntry entry) {
        return new LlmAuditEntryResponse(
                entry.auditId(),
                entry.sessionId(),
                entry.timestamp().toString(),
                entry.provider().value(),
                entry.taskType().name(),
                entry.sanitizationVersion(),
                entry.payloadHash(),
                entry.promptTokensEstimate(),
                entry.degraded(),
                entry.degradationReason());
    }
}
