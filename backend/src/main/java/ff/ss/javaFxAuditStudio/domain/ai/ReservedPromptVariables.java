package ff.ss.javaFxAuditStudio.domain.ai;

import java.util.Map;
import java.util.Set;

/**
 * Variables reservees du template Mustache qui ne doivent pas etre ecrasees par extraContext (AI-6).
 */
public final class ReservedPromptVariables {

    public static final Set<String> RESERVED_KEYS = Set.of(
            "sanitizedSource",
            "controllerRef",
            "taskType",
            "estimatedTokens",
            "bundleId");

    public static void assertNoCollision(final Map<String, Object> extraContext, final String requestId) {
        for (String key : RESERVED_KEYS) {
            if (extraContext.containsKey(key)) {
                throw new ReservedPromptVariableException(key, requestId);
            }
        }
    }

    private ReservedPromptVariables() {}
}
