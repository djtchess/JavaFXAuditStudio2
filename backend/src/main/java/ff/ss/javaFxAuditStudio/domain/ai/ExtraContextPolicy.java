package ff.ss.javaFxAuditStudio.domain.ai;

import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Politique d'allowlist des cles extraContext par TaskType (AI-6-T2).
 */
public final class ExtraContextPolicy {

    private static final Logger LOG = LoggerFactory.getLogger(ExtraContextPolicy.class);

    private static final Map<TaskType, Set<String>> ALLOWED_KEYS = Map.of(
            TaskType.ARTIFACT_REFINEMENT, Set.of(
                    "artifactType", "refineInstruction", "currentArtifactCode",
                    "classifiedRules", "screenContext", "reclassificationFeedback",
                    "projectReferencePatterns"),
            TaskType.SPRING_BOOT_GENERATION, Set.of(
                    "artifactType", "instruction", "previousCode",
                    "classifiedRules", "screenContext", "migrationPlan",
                    "ruleSourceSnippets", "reclassificationFeedback", "projectReferencePatterns"),
            TaskType.ARTIFACT_COHERENCE, Set.of(
                    "classifiedRules", "screenContext", "generatedArtifacts",
                    "generatedArtifactDetails", "reclassificationFeedback", "projectReferencePatterns"),
            TaskType.ARTIFACT_REVIEW, Set.of(
                    "classifiedRules", "screenContext", "reclassificationFeedback"),
            TaskType.NAMING, Set.of("classifiedRules", "screenContext"),
            TaskType.DESCRIPTION, Set.of("classifiedRules", "screenContext"),
            TaskType.CLASSIFICATION_HINT, Set.of("classifiedRules", "screenContext"));

    public static void warnUnexpectedKeys(
            final TaskType taskType,
            final Map<String, Object> extraContext,
            final String requestId) {
        Set<String> allowed = ALLOWED_KEYS.getOrDefault(taskType, Set.of());
        for (String key : extraContext.keySet()) {
            if (!allowed.contains(key)) {
                LOG.warn("ExtraContext key '{}' not in allowlist for taskType={} [requestId={}]",
                        key, taskType, requestId);
            }
        }
    }

    private ExtraContextPolicy() {}
}
