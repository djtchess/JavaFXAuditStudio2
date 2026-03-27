package ff.ss.javaFxAuditStudio.adapters.in.rest;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AiCodeGenerationResponse;
import ff.ss.javaFxAuditStudio.application.ports.in.GenerateSpringBootClassesUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;

/**
 * Controller REST pour la génération IA des classes cibles Spring Boot.
 */
@Tag(name = "Generation IA Spring Boot")
@RestController
@RequestMapping("/api/v1/analyses")
public class AiSpringBootGenerationController {

    private static final Logger LOG = LoggerFactory.getLogger(AiSpringBootGenerationController.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final GenerateSpringBootClassesUseCase generateUseCase;

    public AiSpringBootGenerationController(final GenerateSpringBootClassesUseCase generateUseCase) {
        this.generateUseCase = Objects.requireNonNull(generateUseCase, "generateUseCase must not be null");
    }

    @Operation(summary = "Generation IA des classes Spring Boot")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Generation effectuee"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @PostMapping("/{sessionId}/generate/ai")
    public ResponseEntity<AiCodeGenerationResponse> generate(
            @Parameter(name = "sessionId", required = true)
            @PathVariable final String sessionId) {
        try {
            AiCodeGenerationResult result = generateUseCase.generate(sessionId);
            return ResponseEntity.ok(toResponse(result));
        } catch (IllegalArgumentException exception) {
            LOG.debug("Session introuvable pour generation IA : {}", sessionId);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "/{sessionId}/generate/ai/stream", produces = "text/event-stream")
    public SseEmitter streamGenerate(@PathVariable final String sessionId) {
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> runStreamingGeneration(sessionId, emitter));
        return emitter;
    }

    private void runStreamingGeneration(final String sessionId, final SseEmitter emitter) {
        try {
            emit(emitter, payload("sanitizing", "Preparation et sanitisation du contexte IA", 15, null));
            emit(emitter, payload("sending_to_llm", "Generation Spring Boot en cours", 40, null));

            AiCodeGenerationResult result = generateUseCase.generate(sessionId);

            emit(emitter, payload("parsing_response", "Analyse de la reponse IA", 70, result));
            emitGeneratedChunks(emitter, result);
            emit(emitter, payload("validating", "Validation des artefacts generes", 90, result));
            emit(emitter, payload("complete", "Generation IA terminee", 100, result));
            emitter.complete();
        } catch (IllegalArgumentException exception) {
            completeWithError(emitter, "Session introuvable : " + sessionId);
        } catch (Exception exception) {
            LOG.warn("Erreur pendant le flux SSE de generation IA pour {} : {}", sessionId, exception.getMessage());
            completeWithError(emitter, "Erreur lors du flux de generation IA");
        }
    }

    private void emitGeneratedChunks(final SseEmitter emitter, final AiCodeGenerationResult result)
            throws JsonProcessingException {
        if (!result.degraded()) {
            for (Map.Entry<String, String> entry : result.generatedClasses().entrySet()) {
                emit(emitter, JSON.writeValueAsString(new GenerationStreamPayload(
                        "streaming",
                        "Artefact " + entry.getKey() + " genere",
                        80,
                        entry.getKey(),
                        entry.getValue(),
                        result.generatedClasses(),
                        result.tokensUsed(),
                        result.provider().value(),
                        result.degraded(),
                        result.degradationReason(),
                        result.requestId())));
            }
        }
    }

    private String payload(
            final String stage,
            final String message,
            final int progress,
            final AiCodeGenerationResult result) throws JsonProcessingException {
        return JSON.writeValueAsString(new GenerationStreamPayload(
                stage,
                message,
                progress,
                null,
                null,
                (result != null) ? result.generatedClasses() : null,
                (result != null) ? result.tokensUsed() : null,
                (result != null) ? result.provider().value() : null,
                (result != null) ? result.degraded() : null,
                (result != null) ? result.degradationReason() : null,
                (result != null) ? result.requestId() : null));
    }

    private void emit(final SseEmitter emitter, final String payload) {
        try {
            emitter.send(SseEmitter.event().data(payload));
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible d'envoyer un evenement SSE", exception);
        }
    }

    private void completeWithError(final SseEmitter emitter, final String message) {
        try {
            emit(emitter, JSON.writeValueAsString(new GenerationStreamPayload(
                    "error",
                    message,
                    100,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    message,
                    null)));
            emitter.complete();
        } catch (Exception exception) {
            emitter.completeWithError(exception);
        }
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

    private record GenerationStreamPayload(
            String stage,
            String message,
            int progress,
            String artifactKey,
            String chunk,
            Map<String, String> generatedClasses,
            Integer tokensUsed,
            String provider,
            Boolean degraded,
            String error,
            String requestId) {
    }
}
