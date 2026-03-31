package ff.ss.javaFxAuditStudio.adapters.out.ai;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties;
import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties.PromptContextBudget;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;

/**
 * Reduction deterministe des gros blocs promptables avant rendu du template.
 */
final class PromptContextBudgetSupport {

    private static final Logger LOG = LoggerFactory.getLogger(PromptContextBudgetSupport.class);

    private PromptContextBudgetSupport() {
    }

    static Map<String, Object> budgetContext(
            final AiEnrichmentProperties properties,
            final AiEnrichmentRequest request) {
        PromptContextBudget budget = properties.effectivePromptContextBudget(request.taskType());
        Map<String, Object> context = new HashMap<>();
        context.put("controllerRef", request.bundle().controllerRef());
        context.put("sanitizedSource", budgetValue(
                "sanitizedSource",
                request.bundle().sanitizedSource(),
                budget,
                request));
        context.put("estimatedTokens", request.bundle().estimatedTokens());
        context.put("taskType", request.taskType().name());
        request.extraContext().forEach((key, value) ->
                context.put(key, budgetValue(key, value, budget, request)));
        return context;
    }

    private static Object budgetValue(
            final String key,
            final Object value,
            final PromptContextBudget budget,
            final AiEnrichmentRequest request) {
        Object budgetedValue = value;
        if (value instanceof String stringValue) {
            int limit = resolveLimit(key, budget);
            if (limit > 0 && stringValue.length() > limit) {
                LOG.debug(
                        "Contexte promptable tronque [requestId={}, taskType={}, key={}, originalChars={}, limitChars={}]",
                        request.requestId(),
                        request.taskType(),
                        key,
                        stringValue.length(),
                        limit);
                budgetedValue = stringValue.substring(0, limit);
            }
        }
        return budgetedValue;
    }

    private static int resolveLimit(final String key, final PromptContextBudget budget) {
        return switch (key) {
            case "sanitizedSource", "currentArtifactCode", "previousCode", "ruleSourceSnippets" ->
                budget.maxCodeFragmentChars();
            case "instruction", "refineInstruction" -> budget.maxInstructionChars();
            case "generatedArtifactDetails" -> budget.maxArtifactDetailsChars();
            case "projectReferencePatterns" -> budget.maxReferencePatternsChars();
            default -> 0;
        };
    }
}
