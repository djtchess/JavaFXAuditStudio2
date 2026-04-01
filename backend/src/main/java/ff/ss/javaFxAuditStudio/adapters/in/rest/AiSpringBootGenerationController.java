package ff.ss.javaFxAuditStudio.adapters.in.rest;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AiCodeGenerationResponse;
import ff.ss.javaFxAuditStudio.application.ports.in.GenerateSpringBootClassesUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Controller REST pour la generation IA des classes cibles Spring Boot.
 */
@Tag(name = "Generation IA Spring Boot")
@RestController
@RequestMapping(path = {"/api/v1/analysis/sessions", "/api/v1/analyses"})
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
            @PathVariable final String sessionId,
            @Parameter(description = "Fournisseur LLM cible (optionnel)", required = false)
            @RequestParam(required = false) final String provider) {
        LlmProvider parsedProvider = parseProvider(provider);
        if (provider != null && !provider.isBlank() && parsedProvider == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            AiCodeGenerationResult result = (parsedProvider == null)
                    ? generateUseCase.generate(sessionId)
                    : generateUseCase.generate(sessionId, parsedProvider);
            return ResponseEntity.ok(toResponse(result));
        } catch (IllegalArgumentException exception) {
            LOG.debug("Session introuvable pour generation IA : {}", sessionId);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "/{sessionId}/generate/ai/stream", produces = "text/event-stream")
    public SseEmitter streamGenerate(
            @PathVariable final String sessionId,
            @RequestParam(required = false) final String provider) {
        SseEmitter emitter = new SseEmitter(0L);
        LlmProvider parsedProvider = parseProvider(provider);
        if (provider != null && !provider.isBlank() && parsedProvider == null) {
            completeWithError(emitter, "Provider LLM invalide");
            return emitter;
        }
        CompletableFuture.runAsync(() -> runStreamingGeneration(sessionId, parsedProvider, emitter));
        return emitter;
    }

    private void runStreamingGeneration(
            final String sessionId,
            final LlmProvider provider,
            final SseEmitter emitter) {
        ScheduledExecutorService progressScheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            emit(emitter, payload("sanitizing", "Preparation et sanitisation du contexte IA", 15, null));
            emit(emitter, payload("sending_to_llm", "Generation Spring Boot en cours", 40, null));

            AtomicInteger progressCounter = new AtomicInteger(44);
            ScheduledFuture<?> progressTask = progressScheduler.scheduleAtFixedRate(() -> {
                int current = progressCounter.getAndAdd(4);
                if (current <= 64) {
                    try {
                        String pulse = JSON.writeValueAsString(new GenerationStreamPayload(
                                "sending_to_llm", "Generation Spring Boot en cours...",
                                current, null, null, null, null, null, null, null, null));
                        emitter.send(SseEmitter.event().data(pulse));
                    } catch (Exception ignored) {
                        // emitter already closed or serialization error - stop silently
                    }
                }
            }, 5, 5, TimeUnit.SECONDS);

            AiCodeGenerationResult result = (provider == null)
                    ? generateUseCase.generate(sessionId)
                    : generateUseCase.generate(sessionId, provider);
            progressTask.cancel(false);

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
        } finally {
            progressScheduler.shutdownNow();
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
