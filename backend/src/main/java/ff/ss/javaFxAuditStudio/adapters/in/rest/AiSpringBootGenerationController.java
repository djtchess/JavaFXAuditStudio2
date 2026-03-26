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
import org.springframework.web.bind.annotation.RestController;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AiCodeGenerationResponse;
import ff.ss.javaFxAuditStudio.application.ports.in.GenerateSpringBootClassesUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;

/**
 * Controller REST pour la génération IA des classes cibles Spring Boot (JAS-031).
 *
 * <p>Expose :
 * <ul>
 *   <li>{@code POST /api/v1/analyses/{sessionId}/generate/ai}</li>
 * </ul>
 *
 * <p>Codes de retour :
 * <ul>
 *   <li>200 OK — génération nominale ou dégradée (le champ {@code degraded} le distingue)</li>
 *   <li>404 Not Found — session introuvable</li>
 * </ul>
 */
@Tag(name = "Génération IA Spring Boot", description = "Génération IA des classes cibles Spring Boot depuis les règles classifiées JavaFX")
@RestController
@RequestMapping("/api/v1/analyses")
public class AiSpringBootGenerationController {

    private static final Logger LOG = LoggerFactory.getLogger(AiSpringBootGenerationController.class);

    private final GenerateSpringBootClassesUseCase generateUseCase;

    public AiSpringBootGenerationController(final GenerateSpringBootClassesUseCase generateUseCase) {
        this.generateUseCase = Objects.requireNonNull(generateUseCase);
    }

    /**
     * Génère les classes Spring Boot cibles pour la session d'analyse.
     *
     * <p>Le LLM reçoit le code source sanitisé du contrôleur JavaFX et les règles
     * classifiées, et génère les classes Spring Boot correspondantes (UseCase, ViewModel,
     * Policy, Gateway) en code Java compilable.
     *
     * @param sessionId identifiant de la session d'analyse
     * @return 200 avec le résultat (nominal ou dégradé), 404 si session introuvable
     */
    @Operation(
            summary = "Génération IA des classes Spring Boot",
            description = "Soumet le code source sanitisé et les règles classifiées à un LLM (Claude ou OpenAI) "
                    + "pour générer les classes Spring Boot cibles (UseCase, ViewModel, Policy, Gateway). "
                    + "Retourne un résultat dégradé si le LLM est indisponible ou si aucune classification n'existe.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Génération effectuée (peut être dégradée)"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @PostMapping("/{sessionId}/generate/ai")
    public ResponseEntity<AiCodeGenerationResponse> generate(
            @Parameter(name = "sessionId", description = "Identifiant de la session d'analyse", required = true)
            @PathVariable final String sessionId) {

        LOG.debug("Génération IA Spring Boot demandée — session={}", sessionId);

        try {
            AiCodeGenerationResult result = generateUseCase.generate(sessionId);
            return ResponseEntity.ok(toResponse(result));
        } catch (IllegalArgumentException ex) {
            LOG.debug("Session introuvable pour génération IA : {}", sessionId);
            return ResponseEntity.notFound().build();
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
}
