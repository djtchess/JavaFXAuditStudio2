package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ReclassifyRuleUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ReclassificationAuditEntry;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Service applicatif pour la reclassification manuelle d'une regle de gestion.
 * Verifie le statut LOCKED, met a jour la classification, persiste l'audit
 * et declenche la regeneration des artefacts.
 */
public class ReclassifyRuleService implements ReclassifyRuleUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReclassifyRuleService.class);
    private static final String MDC_CORRELATION_KEY = "correlationId";

    private final AnalysisSessionPort analysisSessionPort;
    private final ClassificationPersistencePort classificationPersistencePort;
    private final ReclassificationAuditPort reclassificationAuditPort;
    private final GenerateArtifactsUseCase generateArtifactsUseCase;

    public ReclassifyRuleService(
            final AnalysisSessionPort analysisSessionPort,
            final ClassificationPersistencePort classificationPersistencePort,
            final ReclassificationAuditPort reclassificationAuditPort,
            final GenerateArtifactsUseCase generateArtifactsUseCase) {
        this.analysisSessionPort = Objects.requireNonNull(analysisSessionPort);
        this.classificationPersistencePort = Objects.requireNonNull(classificationPersistencePort);
        this.reclassificationAuditPort = Objects.requireNonNull(reclassificationAuditPort);
        this.generateArtifactsUseCase = Objects.requireNonNull(generateArtifactsUseCase);
    }

    @Override
    public BusinessRule reclassify(
            final String analysisId,
            final String ruleId,
            final ResponsibilityClass newCategory,
            final String reason) {
        Objects.requireNonNull(analysisId, "analysisId must not be null");
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        Objects.requireNonNull(newCategory, "newCategory must not be null");

        String correlationId = Optional.ofNullable(MDC.get(MDC_CORRELATION_KEY)).orElse("unavailable");
        log.debug("Reclassification demandee [correlationId={}, ruleId masque, analysisId masque]", correlationId);

        AnalysisSession session = loadSession(analysisId);
        checkNotLocked(session);

        ClassificationResult current = loadClassification(analysisId);
        BusinessRule original = findRule(current, ruleId, analysisId);

        BusinessRule updated = rebuildWithNewCategory(original, newCategory);
        ClassificationResult newClassification = replaceRule(current, original, updated);
        classificationPersistencePort.save(analysisId, newClassification);

        persistAuditEntry(analysisId, ruleId, original.responsibilityClass(), newCategory, reason);
        triggerRegeneration(analysisId, session.controllerName());

        log.debug("Reclassification terminee [correlationId={}]", correlationId);
        return updated;
    }

    private AnalysisSession loadSession(final String analysisId) {
        return analysisSessionPort.findById(analysisId)
                .orElseThrow(() -> new NoSuchElementException("Session introuvable : " + analysisId));
    }

    private void checkNotLocked(final AnalysisSession session) {
        if (AnalysisStatus.LOCKED.equals(session.status())) {
            throw new IllegalStateException(
                    "Reclassification impossible : la session est verrouilee (statut LOCKED)");
        }
    }

    private ClassificationResult loadClassification(final String analysisId) {
        return classificationPersistencePort.findBySessionId(analysisId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Classification introuvable pour la session : " + analysisId));
    }

    private BusinessRule findRule(
            final ClassificationResult classification,
            final String ruleId,
            final String analysisId) {
        return allRules(classification).stream()
                .filter(r -> ruleId.equals(r.ruleId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Regle introuvable : " + ruleId + " dans la session : " + analysisId));
    }

    private List<BusinessRule> allRules(final ClassificationResult classification) {
        List<BusinessRule> all = new ArrayList<>(classification.rules());
        all.addAll(classification.uncertainRules());
        return all;
    }

    private BusinessRule rebuildWithNewCategory(
            final BusinessRule original,
            final ResponsibilityClass newCategory) {
        return new BusinessRule(
                original.ruleId(),
                original.description(),
                original.sourceRef(),
                original.sourceLine(),
                newCategory,
                original.extractionCandidate(),
                original.uncertain(),
                original.signature());
    }

    private ClassificationResult replaceRule(
            final ClassificationResult current,
            final BusinessRule original,
            final BusinessRule updated) {
        List<BusinessRule> newCertain = replaceInList(current.rules(), original, updated);
        List<BusinessRule> newUncertain = replaceInList(current.uncertainRules(), original, updated);
        return new ClassificationResult(
                current.controllerRef(),
                newCertain,
                newUncertain,
                current.parsingMode(),
                current.parsingFallbackReason(),
                current.excludedLifecycleMethodsCount());
    }

    private List<BusinessRule> replaceInList(
            final List<BusinessRule> source,
            final BusinessRule original,
            final BusinessRule updated) {
        return source.stream()
                .map(r -> r.ruleId().equals(original.ruleId()) ? updated : r)
                .toList();
    }

    private void persistAuditEntry(
            final String analysisId,
            final String ruleId,
            final ResponsibilityClass fromCategory,
            final ResponsibilityClass toCategory,
            final String reason) {
        ReclassificationAuditEntry entry = new ReclassificationAuditEntry(
                UUID.randomUUID().toString(),
                analysisId,
                ruleId,
                fromCategory,
                toCategory,
                reason,
                Instant.now());
        reclassificationAuditPort.save(entry);
    }

    private void triggerRegeneration(final String analysisId, final String controllerRef) {
        try {
            generateArtifactsUseCase.handle(analysisId, controllerRef);
        } catch (Exception ex) {
            log.warn("Regeneration des artefacts echouee apres reclassification - non bloquant", ex);
        }
    }
}
