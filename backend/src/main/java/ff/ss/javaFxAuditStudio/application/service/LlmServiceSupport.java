package ff.ss.javaFxAuditStudio.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiGeneratedArtifact;
import ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TokenEstimator;
import ff.ss.javaFxAuditStudio.domain.analysis.ControllerDependency;
import ff.ss.javaFxAuditStudio.domain.analysis.DeltaAnalysisSummary;
import ff.ss.javaFxAuditStudio.domain.analysis.StateMachineInsight;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.cartography.FxmlComponent;
import ff.ss.javaFxAuditStudio.domain.cartography.HandlerBinding;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;
import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;
import ff.ss.javaFxAuditStudio.domain.migration.PlannedLot;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ReclassificationAuditEntry;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

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
                SANITIZATION_VERSION,
                null);
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
     * Format a migration plan so it can guide downstream generation prompts.
     */
    public static String formatMigrationPlan(final MigrationPlan migrationPlan) {
        if (migrationPlan == null) {
            return "Aucun plan de migration disponible.";
        }

        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Controller: " + migrationPlan.controllerRef());
        joiner.add("Compilable: " + migrationPlan.compilable());
        for (PlannedLot lot : migrationPlan.lots()) {
            joiner.add("Lot " + lot.lotNumber() + " - " + lot.title() + " : " + lot.objective());
            if (!lot.extractionCandidates().isEmpty()) {
                joiner.add("  Cibles: " + String.join(", ", lot.extractionCandidates()));
            }
            if (!lot.risks().isEmpty()) {
                joiner.add("  Risques: " + lot.risks().stream()
                        .map(risk -> risk.level().name() + " - " + risk.description()
                                + " / mitigation: " + risk.mitigation())
                        .collect(Collectors.joining(" | ")));
            }
        }
        return joiner.toString();
    }

    /**
     * Build focused source snippets for the classified rules so the LLM can preserve
     * the original business or UI decision logic instead of emitting placeholders.
     */
    public static String formatRuleSourceSnippets(
            final String source,
            final ClassificationResult classification) {
        if (source == null || source.isBlank() || classification == null) {
            return "Aucun extrait de source disponible.";
        }

        StringJoiner joiner = new StringJoiner("\n\n");
        int added = 0;
        for (BusinessRule rule : allRules(classification)) {
            if (added >= 8) {
                break;
            }
            String methodName = extractMethodName(rule);
            String snippet = extractMethodSnippet(source, methodName);
            if (methodName != null && !methodName.isBlank() && snippet != null && !snippet.isBlank()) {
                joiner.add("## " + rule.ruleId()
                        + " / " + methodName
                        + optionalSuffix(Integer.toString(rule.sourceLine()), " / line ")
                        + "\n```java\n" + limitSnippet(snippet) + "\n```");
                added++;
            }
        }
        if (added == 0) {
            return "Aucun extrait de methode associe aux regles classifiees.";
        }
        return joiner.toString();
    }

    /**
     * Construit un resume lisible du contexte ecran a injecter dans un prompt LLM.
     */
    public static String formatScreenContext(
            final AnalysisSession session,
            final ClassificationResult classification,
            final ControllerCartography cartography) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(classification, "classification must not be null");

        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Session: " + session.sessionId());
        joiner.add("Controller: " + session.controllerName());
        joiner.add("Source snippet ref: " + safeText(session.sourceSnippetRef()));
        joiner.add("Cartography: " + formatCartographySummary(cartography));
        joiner.add("FXML components: " + formatComponents(cartography));
        joiner.add("Handlers: " + formatHandlers(cartography));
        joiner.add("Dependencies: " + formatDependencies(classification));
        joiner.add("Classification: " + formatClassificationSummary(classification));
        return joiner.toString();
    }

    /**
     * Construit un feedback de reclassification exploitable dans un prompt.
     */
    public static String formatReclassificationFeedback(
            final String sessionId,
            final ClassificationResult classification,
            final ReclassificationAuditPort reclassificationAuditPort) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(classification, "classification must not be null");

        if (reclassificationAuditPort == null) {
            return "Aucun retour de reclassification disponible.";
        }

        List<ReclassificationAuditEntry> entries = collectReclassificationAuditEntries(
                sessionId, classification, reclassificationAuditPort);
        if (entries.isEmpty()) {
            return "Aucune reclassification enregistree pour cette session.";
        }

        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Reclassifications utilisateur:");
        for (ReclassificationAuditEntry entry : entries) {
            joiner.add(formatReclassificationEntry(entry));
        }
        return joiner.toString();
    }

    /**
     * Resume les artefacts generes pour un prompt de coherence.
     */
    public static String formatGeneratedArtifacts(final GenerationResult generationResult) {
        if (generationResult == null || generationResult.artifacts().isEmpty()) {
            return "Aucun artefact genere disponible.";
        }

        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Controller: " + generationResult.controllerRef());
        for (CodeArtifact artifact : generationResult.artifacts()) {
            joiner.add(formatArtifact(artifact));
        }
        if (!generationResult.warnings().isEmpty()) {
            joiner.add("Warnings:");
            for (String warning : generationResult.warnings()) {
                joiner.add("- " + warning);
            }
        }
        return joiner.toString();
    }

    /**
     * Resume les artefacts IA versionnes pour un prompt de coherence.
     */
    public static String formatGeneratedArtifacts(final List<AiGeneratedArtifact> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            return "Aucun artefact IA persiste disponible.";
        }

        StringJoiner joiner = new StringJoiner("\n");
        for (AiGeneratedArtifact artifact : artifacts) {
            joiner.add("- " + artifact.artifactType()
                    + " / " + artifact.className()
                    + " / v" + artifact.versionNumber()
                    + " / " + artifact.originTask().name());
        }
        return joiner.toString();
    }

    /**
     * Concatene les contenus des artefacts IA pour une revue de coherence.
     */
    public static String formatGeneratedArtifactDetails(final List<AiGeneratedArtifact> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            return "Aucun contenu d'artefact IA disponible.";
        }

        StringJoiner joiner = new StringJoiner("\n\n");
        for (AiGeneratedArtifact artifact : artifacts) {
            joiner.add("## " + artifact.artifactType() + " / " + artifact.className() + " / v" + artifact.versionNumber()
                    + "\n```java\n" + artifact.content() + "\n```");
        }
        return joiner.toString();
    }

    /**
     * Formate les patterns projet a injecter dans un prompt IA.
     */
    public static String formatProjectReferencePatterns(final List<ProjectReferencePattern> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return "Aucun pattern projet fourni.";
        }

        StringJoiner joiner = new StringJoiner("\n\n");
        int index = 0;
        for (ProjectReferencePattern pattern : patterns) {
            if (index >= 5) {
                break;
            }
            joiner.add("## " + pattern.artifactType() + " / " + pattern.referenceName()
                    + "\n```java\n" + pattern.content() + "\n```");
            index += 1;
        }
        if (patterns.size() > index) {
            joiner.add("... " + (patterns.size() - index) + " pattern(s) supplementaire(s) non affiches");
        }
        return joiner.toString();
    }

    /**
     * Estimate the number of tokens in a source text with a simple heuristic.
     */
    public static int estimateTokens(final String source) {
        return TokenEstimator.estimate(source);
    }

    private static String formatClassificationSummary(final ClassificationResult classification) {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        joiner.add("rules=" + classification.rules().size());
        joiner.add("uncertain=" + classification.uncertainRules().size());
        joiner.add("stateMachine=" + formatStateMachine(classification.stateMachine()));
        joiner.add("dependencies=" + classification.dependencies().size());
        joiner.add("delta=" + formatDelta(classification.deltaAnalysis()));
        return joiner.toString();
    }

    private static String formatStateMachine(final StateMachineInsight stateMachine) {
        if (stateMachine == null) {
            return "absent";
        }
        return stateMachine.status().name()
                + " confidence=" + String.format("%.2f", stateMachine.confidence())
                + " states=" + stateMachine.states().size()
                + " transitions=" + stateMachine.transitions().size();
    }

    private static String formatDelta(final DeltaAnalysisSummary deltaAnalysis) {
        if (deltaAnalysis == null) {
            return "none";
        }
        return "added=" + deltaAnalysis.addedRules()
                + ", removed=" + deltaAnalysis.removedRules()
                + ", changed=" + deltaAnalysis.changedRules();
    }

    private static String formatCartographySummary(final ControllerCartography cartography) {
        if (cartography == null) {
            return "absent";
        }
        return "fxml=" + cartography.fxmlRef()
                + ", components=" + cartography.components().size()
                + ", handlers=" + cartography.handlers().size()
                + ", unknowns=" + cartography.unknowns().size();
    }

    private static String formatComponents(final ControllerCartography cartography) {
        if (cartography == null || cartography.components().isEmpty()) {
            return "none";
        }
        return summarizeWithOverflow(
                cartography.components().stream()
                        .map(component -> component.componentType() + "#" + component.fxId()
                                + optionalSuffix(component.eventHandler(), " -> "))
                        .toList(),
                8);
    }

    private static String formatHandlers(final ControllerCartography cartography) {
        if (cartography == null || cartography.handlers().isEmpty()) {
            return "none";
        }
        return summarizeWithOverflow(
                cartography.handlers().stream()
                        .map(handler -> handler.methodName() + " [" + handler.injectedType() + "] @ " + handler.fxmlRef())
                        .toList(),
                8);
    }

    private static String formatDependencies(final ClassificationResult classification) {
        if (classification.dependencies().isEmpty()) {
            return "none";
        }
        return summarizeWithOverflow(
                classification.dependencies().stream()
                        .map(dependency -> dependency.kind().name() + ":" + dependency.target()
                                + optionalSuffix(dependency.via(), " via "))
                        .toList(),
                8);
    }

    private static String summarizeWithOverflow(final List<String> values, final int limit) {
        List<String> visible = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(limit)
                .toList();
        String summary = String.join("; ", visible);
        int remaining = values.size() - visible.size();
        if (remaining > 0) {
            summary += "; ... (+" + remaining + " more)";
        }
        return summary;
    }

    private static String extractMethodName(final BusinessRule rule) {
        String description = rule.description();
        if (description.startsWith("Methode handler ")) {
            return extractUntilColon(description.substring("Methode handler ".length()));
        }
        if (description.startsWith("Methode garde ")) {
            return extractUntilColon(description.substring("Methode garde ".length()));
        }
        if (description.startsWith("Methode ")) {
            String tail = description.substring("Methode ".length());
            int parenIndex = tail.indexOf('(');
            if (parenIndex > 0) {
                return tail.substring(0, parenIndex).trim();
            }
        }
        return null;
    }

    private static String extractUntilColon(final String value) {
        int colonIndex = value.indexOf(':');
        String extracted = (colonIndex >= 0) ? value.substring(0, colonIndex) : value;
        return extracted.trim();
    }

    private static String extractMethodSnippet(final String source, final String methodName) {
        if (methodName == null || methodName.isBlank()) {
            return "";
        }
        Pattern pattern = Pattern.compile(
                "(?s)(@FXML\\s+)?(?:public|private|protected)\\s+[\\w<>\\[\\], ?]+\\s+"
                        + Pattern.quote(methodName)
                        + "\\s*\\([^)]*\\)\\s*\\{");
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return "";
        }
        int openBrace = source.indexOf('{', matcher.end() - 1);
        int closeBrace = matchingBrace(source, openBrace);
        if (openBrace < 0 || closeBrace <= openBrace) {
            return "";
        }
        return source.substring(matcher.start(), closeBrace + 1).strip();
    }

    private static int matchingBrace(final String source, final int openBrace) {
        if (openBrace < 0 || openBrace >= source.length()) {
            return -1;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;
        char quote = '\0';
        for (int index = openBrace; index < source.length(); index++) {
            char current = source.charAt(index);
            if (inString) {
                if (escaping) {
                    escaping = false;
                    continue;
                }
                if (current == '\\') {
                    escaping = true;
                } else if (current == quote) {
                    inString = false;
                }
                continue;
            }
            if (current == '"' || current == '\'') {
                inString = true;
                quote = current;
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static String limitSnippet(final String snippet) {
        int maxLength = 1600;
        if (snippet.length() <= maxLength) {
            return snippet;
        }
        return snippet.substring(0, maxLength) + "\n// ... extrait tronque";
    }

    private static String optionalSuffix(final String value, final String prefix) {
        return (value != null && !value.isBlank()) ? prefix + value : "";
    }

    private static List<ReclassificationAuditEntry> collectReclassificationAuditEntries(
            final String sessionId,
            final ClassificationResult classification,
            final ReclassificationAuditPort reclassificationAuditPort) {
        List<ReclassificationAuditEntry> entries = new ArrayList<>();
        List<ReclassificationAuditEntry> sessionEntries = reclassificationAuditPort.findByAnalysisId(sessionId);
        if (sessionEntries != null && !sessionEntries.isEmpty()) {
            entries.addAll(sessionEntries);
        } else {
            for (BusinessRule rule : allRules(classification)) {
                entries.addAll(reclassificationAuditPort.findByAnalysisIdAndRuleId(sessionId, rule.ruleId()));
            }
        }
        entries.sort(Comparator.comparing(ReclassificationAuditEntry::timestamp));
        return entries;
    }

    private static List<BusinessRule> allRules(final ClassificationResult classification) {
        List<BusinessRule> all = new ArrayList<>(classification.rules());
        all.addAll(classification.uncertainRules());
        return all;
    }

    private static String formatReclassificationEntry(final ReclassificationAuditEntry entry) {
        return "- " + entry.ruleId()
                + ": " + entry.fromCategory().name()
                + " -> " + entry.toCategory().name()
                + " @ " + safeInstant(entry.timestamp())
                + " | reason=" + safeText(entry.reason());
    }

    private static String formatArtifact(final CodeArtifact artifact) {
        return "- " + artifact.type().name()
                + " / " + artifact.className()
                + " / lot=" + artifact.lotNumber()
                + " / bridge=" + artifact.transitionalBridge();
    }

    private static String safeInstant(final Instant value) {
        return value != null ? value.toString() : "unknown";
    }

    private static String safeText(final String value) {
        return (value != null && !value.isBlank()) ? value : "absent";
    }
}
