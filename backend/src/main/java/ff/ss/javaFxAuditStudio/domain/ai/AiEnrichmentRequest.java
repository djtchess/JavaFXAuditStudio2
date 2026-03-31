package ff.ss.javaFxAuditStudio.domain.ai;

import java.util.Map;
import java.util.Objects;

/**
 * Requête d'enrichissement IA (IAP-2).
 *
 * <p>Le bundle contenu a été préalablement sanitisé.
 * Le champ {@code taskType} est désormais typesafe via {@link TaskType}.
 *
 * @param requestId      UUID de corrélation
 * @param bundle         Bundle sanitisé à enrichir
 * @param taskType       Type de tâche IA (enum typesafe)
 * @param promptTemplate Nom du template Mustache à utiliser
 * @param extraContext   Contexte supplémentaire injecté dans le template Mustache (nullable)
 */
public record AiEnrichmentRequest(
        String requestId,
        SanitizedBundle bundle,
        TaskType taskType,
        String promptTemplate,
        Map<String, Object> extraContext) {

    public AiEnrichmentRequest {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(bundle, "bundle must not be null");
        Objects.requireNonNull(taskType, "taskType must not be null");
        Objects.requireNonNull(promptTemplate, "promptTemplate must not be null");
        extraContext = extraContext != null ? Map.copyOf(extraContext) : Map.of();
        ReservedPromptVariables.assertNoCollision(extraContext, requestId);
        ExtraContextPolicy.warnUnexpectedKeys(taskType, extraContext, requestId);
    }

    /**
     * Constructeur de rétro-compatibilité à 4 arguments sans contexte supplémentaire.
     */
    public AiEnrichmentRequest(
            final String requestId,
            final SanitizedBundle bundle,
            final TaskType taskType,
            final String promptTemplate) {
        this(requestId, bundle, taskType, promptTemplate, null);
    }
}
