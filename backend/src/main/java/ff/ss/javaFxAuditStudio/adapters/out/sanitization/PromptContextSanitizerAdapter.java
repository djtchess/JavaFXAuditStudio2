package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.out.PromptContextSanitizerPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiGeneratedArtifact;
import ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;

/**
 * Adapter de sanitisation du contexte promptable (AI-5).
 *
 * <p>Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration}.
 * Si {@code sanitizationPort} est null, les methodes code retournent le code brut (mode degrade).
 */
public class PromptContextSanitizerAdapter implements PromptContextSanitizerPort {

    private static final Logger LOG = LoggerFactory.getLogger(PromptContextSanitizerAdapter.class);

    private static final int MAX_REFERENCE_PATTERNS = 5;

    private static final Pattern INJECTION_MARKER_PATTERN = Pattern.compile(
            "(?i)(ignore\\s+previous|ignore\\s+all|end\\s+of\\s+system|---+\\s*END"
            + "|<\\s*/?(system|instructions?)>|\\[SYSTEM\\]|\\[INST\\])");

    private final SanitizationPort sanitizationPort;

    public PromptContextSanitizerAdapter(final SanitizationPort sanitizationPort) {
        this.sanitizationPort = sanitizationPort;
    }

    @Override
    public String sanitizeCodeFragment(
            final String requestId,
            final String rawCode,
            final String contextLabel) {
        if (rawCode == null || rawCode.isBlank()) {
            return "";
        }
        if (sanitizationPort == null) {
            return rawCode;
        }
        try {
            return sanitizationPort
                    .sanitize(requestId + "-ctx-" + contextLabel, rawCode, contextLabel)
                    .sanitizedSource();
        } catch (SanitizationRefusedException e) {
            LOG.warn("Sanitisation refusee pour fragment {} (label={}): {}", requestId, contextLabel, e.getMessage());
            return "[contenu sanitise - confidentiel]";
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    @Override
    public String sanitizeInstruction(final String instruction, final int maxLength) {
        if (instruction == null || instruction.isBlank()) {
            return "";
        }
        if (INJECTION_MARKER_PATTERN.matcher(instruction).find()) {
            LOG.warn("Marqueur d'injection detecte dans l'instruction - contenu rejete");
            return "[instruction rejetee - marqueur d injection detecte]";
        }
        if (instruction.length() <= maxLength) {
            return instruction;
        }
        return instruction.substring(0, maxLength);
    }

    @Override
    public String sanitizeArtifactDetails(
            final String requestId,
            final List<AiGeneratedArtifact> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            return "Aucun contenu d'artefact IA disponible.";
        }

        StringJoiner joiner = new StringJoiner("\n\n");
        for (int i = 0; i < artifacts.size(); i++) {
            AiGeneratedArtifact artifact = artifacts.get(i);
            String sanitizedContent = sanitizeCodeFragment(
                    requestId + "-artifact-" + i,
                    artifact.content(),
                    artifact.artifactType());
            joiner.add("## " + artifact.artifactType()
                    + " / " + artifact.className()
                    + " / v" + artifact.versionNumber()
                    + "\n```java\n" + sanitizedContent + "\n```");
        }
        return joiner.toString();
    }

    @Override
    public String sanitizeReferencePatterns(
            final String requestId,
            final List<ProjectReferencePattern> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return "Aucun pattern projet fourni.";
        }

        StringJoiner joiner = new StringJoiner("\n\n");
        int limit = Math.min(patterns.size(), MAX_REFERENCE_PATTERNS);
        for (int i = 0; i < limit; i++) {
            ProjectReferencePattern pattern = patterns.get(i);
            String sanitizedContent = sanitizeCodeFragment(
                    requestId + "-pattern-" + i,
                    pattern.content(),
                    pattern.artifactType());
            joiner.add("## " + pattern.artifactType()
                    + " / " + pattern.referenceName()
                    + "\n```java\n" + sanitizedContent + "\n```");
        }
        if (patterns.size() > limit) {
            joiner.add("... " + (patterns.size() - limit) + " pattern(s) supplementaire(s) non affiches");
        }
        return joiner.toString();
    }
}
