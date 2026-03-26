package ff.ss.javaFxAuditStudio.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TokenEstimator;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;

/**
 * Shared utility methods for LLM-facing application services.
 */
public final class LlmServiceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(LlmServiceSupport.class);

    /** Version of the sanitization profile applied by the current pipeline. */
    public static final String SANITIZATION_VERSION = "1.0";

    private LlmServiceSupport() {
        // Utility class.
    }

    /**
     * Resolve source content through the dedicated port and preserve the historical
     * fallback to the controller reference when the source cannot be read.
     */
    public static String readSourceFile(
            final String controllerRef,
            final SourceFileReaderPort sourceFileReaderPort) {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");

        String rawSource = controllerRef;
        if (sourceFileReaderPort != null) {
            rawSource = sourceFileReaderPort.read(controllerRef).orElse(controllerRef);
        }
        return rawSource;
    }

    /**
     * Build a sanitized bundle from raw source code.
     */
    public static SanitizedBundle buildBundle(
            final String bundleId,
            final String rawSource,
            final String controllerRef,
            final SanitizationPort sanitizationPort) {
        Objects.requireNonNull(bundleId, "bundleId must not be null");
        Objects.requireNonNull(rawSource, "rawSource must not be null");
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");

        if (sanitizationPort != null) {
            return sanitizationPort.sanitize(bundleId, rawSource, controllerRef);
        }

        LOG.debug("SanitizationPort absent - bundle built without sanitization for {}", controllerRef);
        return new SanitizedBundle(
                bundleId,
                controllerRef,
                rawSource,
                estimateTokens(rawSource),
                SANITIZATION_VERSION);
    }

    /**
     * Format classified rules for prompt injection.
     */
    public static String formatRules(final ClassificationResult classification) {
        Objects.requireNonNull(classification, "classification must not be null");

        List<BusinessRule> all = new ArrayList<>(classification.rules());
        all.addAll(classification.uncertainRules());
        return all.stream()
                .map(rule -> String.format("[%s] %s (line %d) -> %s / %s%s",
                        rule.ruleId(),
                        rule.description(),
                        rule.sourceLine(),
                        rule.extractionCandidate().name(),
                        rule.responsibilityClass().name(),
                        rule.uncertain() ? " WARNING UNCERTAIN" : ""))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Estimate the number of tokens in a source text with a simple heuristic.
     */
    public static int estimateTokens(final String source) {
        return TokenEstimator.estimate(source);
    }
}
