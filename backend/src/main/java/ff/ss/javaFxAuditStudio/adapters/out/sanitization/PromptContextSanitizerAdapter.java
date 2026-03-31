package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.out.PromptContextSanitizerPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties;
import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties.PromptContextBudget;
import ff.ss.javaFxAuditStudio.domain.ai.AiGeneratedArtifact;
import ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;

/**
 * Adapter de sanitisation du contexte promptable (AI-5 / AI-8).
 *
 * <p>Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration}.
 * Si {@code sanitizationPort} est null, les methodes retournent le contenu brut en mode degrade.
 */
public class PromptContextSanitizerAdapter implements PromptContextSanitizerPort {

    private static final Logger LOG = LoggerFactory.getLogger(PromptContextSanitizerAdapter.class);

    private static final Pattern INJECTION_MARKER_PATTERN = Pattern.compile(
            "(?i)(ignore\\s+previous|ignore\\s+all|end\\s+of\\s+system|---+\\s*END"
            + "|<\\s*/?(system|instructions?)>|\\[SYSTEM\\]|\\[INST\\])");

    private final SanitizationPort sanitizationPort;
    private final AiEnrichmentProperties properties;

    public PromptContextSanitizerAdapter(final SanitizationPort sanitizationPort) {
        this(sanitizationPort, null);
    }

    public PromptContextSanitizerAdapter(
            final SanitizationPort sanitizationPort,
            final AiEnrichmentProperties properties) {
        this.sanitizationPort = sanitizationPort;
        this.properties = properties;
    }

    public String sanitizeCodeFragment(
            final String requestId,
            final String rawCode,
            final String contextLabel) {
        return sanitizeCodeFragment(requestId, null, rawCode, contextLabel);
    }

    @Override
    public String sanitizeCodeFragment(
            final String requestId,
            final TaskType taskType,
            final String rawCode,
            final String contextLabel) {
        if (rawCode == null || rawCode.isBlank()) {
            return "";
        }

        String sanitized = rawCode;
        if (sanitizationPort != null) {
            try {
                sanitized = sanitizationPort
                        .sanitize(requestId + "-ctx-" + contextLabel, rawCode, contextLabel)
                        .sanitizedSource();
            } catch (SanitizationRefusedException exception) {
                LOG.warn(
                        "Sanitisation refusee pour fragment [requestId={}, label={}, taskType={}]: {}",
                        requestId, contextLabel, normalizedTaskType(taskType), exception.getMessage());
                return "[contenu sanitise - confidentiel]";
            } catch (IllegalArgumentException exception) {
                return "";
            }
        }

        int limit = effectiveBudget(taskType).maxCodeFragmentChars();
        String limited = truncate(sanitized, limit);
        if (limited.length() < sanitized.length()) {
            LOG.debug(
                    "Fragment promptable tronque [requestId={}, label={}, taskType={}, originalChars={}, limitChars={}]",
                    requestId, contextLabel, normalizedTaskType(taskType), sanitized.length(), limit);
        }
        return limited;
    }

    public String sanitizeInstruction(final String instruction, final int maxLength) {
        return sanitizeInstruction(null, null, instruction, maxLength);
    }

    @Override
    public String sanitizeInstruction(
            final String requestId,
            final TaskType taskType,
            final String instruction,
            final int maxLength) {
        if (instruction == null || instruction.isBlank()) {
            return "";
        }
        if (INJECTION_MARKER_PATTERN.matcher(instruction).find()) {
            LOG.warn(
                    "Marqueur d'injection detecte dans l'instruction [requestId={}, taskType={}]",
                    requestId, normalizedTaskType(taskType));
            return "[instruction rejetee - marqueur d injection detecte]";
        }

        PromptContextBudget budget = effectiveBudget(taskType);
        int requestedLimit = maxLength > 0 ? maxLength : budget.maxInstructionChars();
        int limit = Math.min(requestedLimit, budget.maxInstructionChars());
        String limited = truncate(instruction, limit);
        if (limited.length() < instruction.length()) {
            LOG.debug(
                    "Instruction promptable tronquee [requestId={}, taskType={}, originalChars={}, limitChars={}]",
                    requestId, normalizedTaskType(taskType), instruction.length(), limit);
        }
        return limited;
    }

    public String sanitizeArtifactDetails(
            final String requestId,
            final List<AiGeneratedArtifact> artifacts) {
        return sanitizeArtifactDetails(requestId, null, artifacts);
    }

    @Override
    public String sanitizeArtifactDetails(
            final String requestId,
            final TaskType taskType,
            final List<AiGeneratedArtifact> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            return "Aucun contenu d'artefact IA disponible.";
        }

        PromptContextBudget budget = effectiveBudget(taskType);
        int maxItems = budget.maxArtifactDetailsItems();
        int maxChars = budget.maxArtifactDetailsChars();
        int maxContentChars = Math.max(1, Math.min(
                budget.maxCodeFragmentChars(),
                Math.max(1, maxChars / Math.max(1, maxItems) - 64)));

        StringJoiner joiner = new StringJoiner("\n\n");
        int processed = 0;
        int truncatedItems = 0;
        for (int index = 0; index < artifacts.size() && processed < maxItems; index++) {
            AiGeneratedArtifact artifact = artifacts.get(index);
            String sanitizedContent = sanitizeCodeFragment(
                    requestId + "-artifact-" + index,
                    taskType,
                    artifact.content(),
                    artifact.artifactType());
            String limitedContent = truncate(sanitizedContent, maxContentChars);
            if (limitedContent.length() < sanitizedContent.length()) {
                truncatedItems++;
            }
            joiner.add(renderMarkdownBlock(
                    artifact.artifactType(),
                    artifact.className(),
                    artifact.versionNumber(),
                    limitedContent));
            processed++;
        }

        String rendered = joiner.toString();
        String limited = truncate(rendered, maxChars);
        if (limited.length() < rendered.length()) {
            LOG.debug(
                    "Details d'artefacts tronques [requestId={}, taskType={}, originalChars={}, limitChars={}, items={}, truncatedItems={}]",
                    requestId, normalizedTaskType(taskType), rendered.length(), maxChars, processed, truncatedItems);
        } else if (artifacts.size() > processed) {
            LOG.debug(
                    "Details d'artefacts limites par budget [requestId={}, taskType={}, requestedItems={}, keptItems={}]",
                    requestId, normalizedTaskType(taskType), artifacts.size(), processed);
        }
        return limited;
    }

    public String sanitizeReferencePatterns(
            final String requestId,
            final List<ProjectReferencePattern> patterns) {
        return sanitizeReferencePatterns(requestId, null, patterns);
    }

    @Override
    public String sanitizeReferencePatterns(
            final String requestId,
            final TaskType taskType,
            final List<ProjectReferencePattern> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return "Aucun pattern projet fourni.";
        }

        PromptContextBudget budget = effectiveBudget(taskType);
        int maxItems = budget.maxReferencePatternsItems();
        int maxChars = budget.maxReferencePatternsChars();
        int maxContentChars = Math.max(1, Math.min(
                budget.maxCodeFragmentChars(),
                Math.max(1, maxChars / Math.max(1, maxItems) - 64)));

        StringJoiner joiner = new StringJoiner("\n\n");
        int processed = 0;
        int truncatedItems = 0;
        for (int index = 0; index < patterns.size() && processed < maxItems; index++) {
            ProjectReferencePattern pattern = patterns.get(index);
            String sanitizedContent = sanitizeCodeFragment(
                    requestId + "-pattern-" + index,
                    taskType,
                    pattern.content(),
                    pattern.artifactType());
            String limitedContent = truncate(sanitizedContent, maxContentChars);
            if (limitedContent.length() < sanitizedContent.length()) {
                truncatedItems++;
            }
            joiner.add(renderMarkdownBlock(
                    pattern.artifactType(),
                    pattern.referenceName(),
                    null,
                    limitedContent));
            processed++;
        }

        String rendered = joiner.toString();
        String limited = truncate(rendered, maxChars);
        if (limited.length() < rendered.length()) {
            LOG.debug(
                    "Patterns projet tronques [requestId={}, taskType={}, originalChars={}, limitChars={}, items={}, truncatedItems={}]",
                    requestId, normalizedTaskType(taskType), rendered.length(), maxChars, processed, truncatedItems);
        } else if (patterns.size() > processed) {
            LOG.debug(
                    "Patterns projet limites par budget [requestId={}, taskType={}, requestedItems={}, keptItems={}]",
                    requestId, normalizedTaskType(taskType), patterns.size(), processed);
        }
        return limited;
    }

    private PromptContextBudget effectiveBudget(final TaskType taskType) {
        TaskType effectiveTaskType = normalizedTaskType(taskType);
        if (properties != null) {
            return properties.effectivePromptContextBudget(effectiveTaskType);
        }
        return defaultBudget(effectiveTaskType);
    }

    private static TaskType normalizedTaskType(final TaskType taskType) {
        return taskType != null ? taskType : TaskType.CLASSIFICATION_HINT;
    }

    private static PromptContextBudget defaultBudget(final TaskType taskType) {
        return switch (normalizedTaskType(taskType)) {
            case NAMING, DESCRIPTION, CLASSIFICATION_HINT -> new PromptContextBudget(
                    4_000, 1_000, 2_000, 2, 2_000, 2);
            case ARTIFACT_REVIEW -> new PromptContextBudget(
                    6_000, 1_500, 8_000, 4, 6_000, 4);
            case ARTIFACT_REFINEMENT -> new PromptContextBudget(
                    10_000, 2_000, 8_000, 3, 6_000, 4);
            case ARTIFACT_COHERENCE -> new PromptContextBudget(
                    8_000, 1_500, 12_000, 5, 8_000, 5);
            case SPRING_BOOT_GENERATION -> new PromptContextBudget(
                    12_000, 1_500, 8_000, 3, 10_000, 5);
        };
    }

    private static String renderMarkdownBlock(
            final String title,
            final String secondaryLabel,
            final Integer version,
            final String content) {
        StringBuilder builder = new StringBuilder();
        builder.append("## ").append(title).append(" / ").append(secondaryLabel);
        if (version != null) {
            builder.append(" / v").append(version);
        }
        builder.append("\n```java\n").append(content).append("\n```");
        return builder.toString();
    }

    private static String truncate(final String value, final int maxLength) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
