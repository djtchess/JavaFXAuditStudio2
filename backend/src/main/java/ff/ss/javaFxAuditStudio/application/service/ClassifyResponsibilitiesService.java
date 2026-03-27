package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.ClassifyResponsibilitiesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceReaderPort;
import ff.ss.javaFxAuditStudio.domain.analysis.DeltaAnalysisSummary;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionResult;
import ff.ss.javaFxAuditStudio.domain.rules.ParsingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ClassifyResponsibilitiesService implements ClassifyResponsibilitiesUseCase {

    private static final Logger log = LoggerFactory.getLogger(ClassifyResponsibilitiesService.class);

    private final RuleExtractionPort ruleExtractionPort;
    private final ClassificationPersistencePort classificationPersistencePort;
    private final SourceReaderPort sourceReaderPort;

    public ClassifyResponsibilitiesService(
            final RuleExtractionPort ruleExtractionPort,
            final ClassificationPersistencePort classificationPersistencePort,
            final SourceReaderPort sourceReaderPort) {
        this.ruleExtractionPort = Objects.requireNonNull(ruleExtractionPort);
        this.classificationPersistencePort = Objects.requireNonNull(classificationPersistencePort);
        this.sourceReaderPort = Objects.requireNonNull(sourceReaderPort);
    }

    @Override
    public ClassificationResult handle(final String sessionId, final String controllerRef) {
        Optional<ClassificationResult> cached = classificationPersistencePort.findBySessionId(sessionId);
        String javaContent = readJavaContent(controllerRef);
        if (cached.isPresent()) {
            if (javaContent.isBlank()) {
                return cached.get();
            }
            ExtractionResult liveExtraction = ruleExtractionPort.extract(controllerRef, javaContent);
            DeltaAnalysisSummary deltaAnalysis = computeDeltaAnalysis(cached.get(), liveExtraction.rules());
            return new ClassificationResult(
                    cached.get().controllerRef(),
                    cached.get().rules(),
                    cached.get().uncertainRules(),
                    cached.get().parsingMode(),
                    cached.get().parsingFallbackReason(),
                    cached.get().excludedLifecycleMethodsCount(),
                    liveExtraction.stateMachine(),
                    liveExtraction.dependencies(),
                    deltaAnalysis);
        }

        ExtractionResult extraction = ruleExtractionPort.extract(controllerRef, javaContent);
        List<BusinessRule> allRules = extraction.rules();
        ParsingMode parsingMode = extraction.parsingMode();
        String fallbackReason = extraction.fallbackReason();
        int excludedCount = extraction.excludedLifecycleMethodsCount();

        if (excludedCount > 0) {
            log.info("Methodes lifecycle exclues de la classification : {} - ref={}",
                    excludedCount, controllerRef);
        }

        List<BusinessRule> certain = allRules.stream().filter(rule -> !rule.uncertain()).toList();
        List<BusinessRule> uncertain = allRules.stream().filter(BusinessRule::uncertain).toList();
        ClassificationResult result = new ClassificationResult(
                controllerRef,
                certain,
                uncertain,
                parsingMode,
                fallbackReason,
                excludedCount,
                extraction.stateMachine(),
                extraction.dependencies(),
                DeltaAnalysisSummary.none());

        classificationPersistencePort.save(sessionId, result);

        log.debug("Classification terminee - {} regles certaines, {} incertaines, mode={}, lifecycle_exclus={}",
                certain.size(), uncertain.size(), parsingMode, excludedCount);
        return result;
    }

    private String readJavaContent(final String controllerRef) {
        if (controllerRef == null || controllerRef.isBlank()) {
            return "";
        }
        return sourceReaderPort.read(controllerRef)
                .map(input -> input.content())
                .orElseGet(() -> {
                    log.warn("Controller introuvable pour la classification - ref={}", controllerRef);
                    return "";
                });
    }

    private DeltaAnalysisSummary computeDeltaAnalysis(
            final ClassificationResult cached,
            final List<BusinessRule> liveRules) {
        LinkedHashMap<String, BusinessRule> cachedRules = new LinkedHashMap<>();
        LinkedHashMap<String, BusinessRule> currentRules = new LinkedHashMap<>();
        cached.rules().forEach(rule -> cachedRules.put(ruleFingerprint(rule), rule));
        cached.uncertainRules().forEach(rule -> cachedRules.put(ruleFingerprint(rule), rule));
        liveRules.forEach(rule -> currentRules.put(ruleFingerprint(rule), rule));

        int added = 0;
        int removed = 0;
        int changed = 0;

        for (String fingerprint : currentRules.keySet()) {
            if (!cachedRules.containsKey(fingerprint)) {
                added++;
            } else if (hasClassificationChanged(cachedRules.get(fingerprint), currentRules.get(fingerprint))) {
                changed++;
            }
        }
        for (String fingerprint : cachedRules.keySet()) {
            if (!currentRules.containsKey(fingerprint)) {
                removed++;
            }
        }
        return new DeltaAnalysisSummary(added, removed, changed);
    }

    private boolean hasClassificationChanged(final BusinessRule cachedRule, final BusinessRule liveRule) {
        return cachedRule.extractionCandidate() != liveRule.extractionCandidate()
                || cachedRule.responsibilityClass() != liveRule.responsibilityClass()
                || cachedRule.uncertain() != liveRule.uncertain();
    }

    private String ruleFingerprint(final BusinessRule rule) {
        String description = rule.description();
        if (description.startsWith("Methode handler ")) {
            return description.substring("Methode handler ".length()).split(":")[0].trim();
        }
        if (description.startsWith("Methode garde ")) {
            return description.substring("Methode garde ".length()).split(":")[0].trim();
        }
        if (description.startsWith("Champ FXML ")) {
            String[] parts = description.substring("Champ FXML ".length()).split(":")[0].trim().split("\\s+");
            if (parts.length >= 2) {
                return parts[parts.length - 1];
            }
        }
        if (description.startsWith("Service injecte ")) {
            String[] parts = description.substring("Service injecte ".length()).split(":")[0].trim().split("\\s+");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        return description;
    }
}
