package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.ProduceRestitutionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.RestitutionPersistencePort;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;
import ff.ss.javaFxAuditStudio.domain.restitution.ConfidenceLevel;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionSummary;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ProduceRestitutionService implements ProduceRestitutionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProduceRestitutionService.class);

    private final RestitutionPersistencePort restitutionPersistencePort;
    private final ClassificationPersistencePort classificationPersistencePort;
    private final ArtifactPersistencePort artifactPersistencePort;

    public ProduceRestitutionService(
            final RestitutionPersistencePort restitutionPersistencePort,
            final ClassificationPersistencePort classificationPersistencePort,
            final ArtifactPersistencePort artifactPersistencePort) {
        this.restitutionPersistencePort = Objects.requireNonNull(restitutionPersistencePort);
        this.classificationPersistencePort = Objects.requireNonNull(classificationPersistencePort);
        this.artifactPersistencePort = Objects.requireNonNull(artifactPersistencePort);
    }

    @Override
    public RestitutionReport handle(final String sessionId, final String controllerRef) {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");

        Optional<RestitutionReport> cached = restitutionPersistencePort.findBySessionId(sessionId);
        if (cached.isPresent()) {
            return cached.get();
        }

        Optional<ClassificationResult> classifOpt = classificationPersistencePort.findBySessionId(sessionId);
        Optional<GenerationResult> generationOpt = artifactPersistencePort.findBySessionId(sessionId);

        int ruleCount = classifOpt.map(c -> c.rules().size() + c.uncertainRules().size()).orElse(0);
        int uncertainCount = classifOpt.map(c -> c.uncertainRules().size()).orElse(0);
        int certainCount = classifOpt.map(c -> c.rules().size()).orElse(0);
        int artifactCount = generationOpt.map(g -> g.artifacts().size()).orElse(0);

        ConfidenceLevel confidence = determineConfidence(certainCount, uncertainCount, artifactCount);

        RestitutionSummary summary = new RestitutionSummary(
                controllerRef, ruleCount, uncertainCount, artifactCount, 0, confidence, false);

        List<String> findings = buildFindings(classifOpt, generationOpt);

        RestitutionReport report = new RestitutionReport(summary, List.of(), List.of(), findings);

        restitutionPersistencePort.save(sessionId, report);
        log.debug("Restitution terminee - {} regles, {} artefacts, confiance={}", ruleCount, artifactCount, confidence);
        return report;
    }

    private ConfidenceLevel determineConfidence(final int certain, final int uncertain, final int artifacts) {
        if (certain == 0 && uncertain == 0) {
            return ConfidenceLevel.INSUFFICIENT;
        }
        if (artifacts == 0) {
            return ConfidenceLevel.LOW;
        }
        if (uncertain > certain) {
            return ConfidenceLevel.MEDIUM;
        }
        return ConfidenceLevel.HIGH;
    }

    private List<String> buildFindings(
            final Optional<ClassificationResult> classifOpt,
            final Optional<GenerationResult> generationOpt) {
        var findings = new java.util.ArrayList<String>();
        if (classifOpt.isEmpty()) {
            findings.add("Classification non disponible pour cette session");
        }
        if (generationOpt.isEmpty()) {
            findings.add("Generation d'artefacts non disponible pour cette session");
        }
        return List.copyOf(findings);
    }
}
