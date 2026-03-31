package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.ProduceRestitutionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.MigrationPlanPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.RestitutionPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.RestitutionFormatterPort;
import ff.ss.javaFxAuditStudio.domain.cartography.CartographyUnknown;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;
import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;
import ff.ss.javaFxAuditStudio.domain.migration.PlannedLot;
import ff.ss.javaFxAuditStudio.domain.migration.RegressionRisk;
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
    private final CartographyPersistencePort cartographyPersistencePort;
    private final MigrationPlanPersistencePort migrationPlanPersistencePort;
    private final RestitutionFormatterPort restitutionFormatterPort;

    public ProduceRestitutionService(
            final RestitutionPersistencePort restitutionPersistencePort,
            final ClassificationPersistencePort classificationPersistencePort,
            final ArtifactPersistencePort artifactPersistencePort,
            final CartographyPersistencePort cartographyPersistencePort,
            final MigrationPlanPersistencePort migrationPlanPersistencePort,
            final RestitutionFormatterPort restitutionFormatterPort) {
        this.restitutionPersistencePort = Objects.requireNonNull(restitutionPersistencePort);
        this.classificationPersistencePort = Objects.requireNonNull(classificationPersistencePort);
        this.artifactPersistencePort = Objects.requireNonNull(artifactPersistencePort);
        this.cartographyPersistencePort = Objects.requireNonNull(cartographyPersistencePort);
        this.migrationPlanPersistencePort = Objects.requireNonNull(migrationPlanPersistencePort);
        this.restitutionFormatterPort = Objects.requireNonNull(restitutionFormatterPort);
    }

    @Override
    public RestitutionReport handle(final String sessionId, final String controllerRef) {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");

        Optional<RestitutionReport> cached = restitutionPersistencePort.findBySessionId(sessionId);
        if (cached.isPresent()) {
            return ensurePersistedMarkdown(sessionId, cached.get());
        }

        Optional<ControllerCartography> cartographyOpt = cartographyPersistencePort.findBySessionId(sessionId);
        Optional<ClassificationResult> classifOpt = classificationPersistencePort.findBySessionId(sessionId);
        Optional<MigrationPlan> migrationPlanOpt = migrationPlanPersistencePort.findBySessionId(sessionId);
        Optional<GenerationResult> generationOpt = artifactPersistencePort.findBySessionId(sessionId);

        int ruleCount = classifOpt.map(c -> c.rules().size() + c.uncertainRules().size()).orElse(0);
        int uncertainCount = classifOpt.map(c -> c.uncertainRules().size()).orElse(0);
        int certainCount = classifOpt.map(c -> c.rules().size()).orElse(0);
        int artifactCount = generationOpt.map(g -> g.artifacts().size()).orElse(0);

        ConfidenceLevel confidence = determineConfidence(certainCount, uncertainCount, artifactCount);

        RestitutionSummary summary = new RestitutionSummary(
                controllerRef, ruleCount, uncertainCount, artifactCount, 0, confidence, false);

        List<String> unknowns = buildUnknowns(cartographyOpt);
        List<String> findings = buildFindings(classifOpt, generationOpt);
        List<String> lotSummaries = buildLotSummaries(migrationPlanOpt);
        List<String> artifactSummaries = buildArtifactSummaries(generationOpt);

        RestitutionReport report = new RestitutionReport(
                summary,
                List.of(),
                unknowns,
                findings,
                lotSummaries,
                artifactSummaries,
                "");
        RestitutionReport reportWithMarkdown = withMarkdown(report);
        RestitutionReport savedReport = restitutionPersistencePort.save(sessionId, reportWithMarkdown);
        log.debug("Restitution terminee - {} regles, {} artefacts, confiance={}", ruleCount, artifactCount, confidence);
        return savedReport;
    }

    private RestitutionReport ensurePersistedMarkdown(
            final String sessionId,
            final RestitutionReport report) {
        if (!report.markdown().isBlank()) {
            return report;
        }
        RestitutionReport enrichedReport = withMarkdown(report);
        return restitutionPersistencePort.save(sessionId, enrichedReport);
    }

    private RestitutionReport withMarkdown(final RestitutionReport report) {
        String markdown = restitutionFormatterPort.formatAsMarkdown(report);
        return new RestitutionReport(
                report.summary(),
                report.contradictions(),
                report.unknowns(),
                report.findings(),
                report.lotSummaries(),
                report.artifactSummaries(),
                markdown);
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
        } else if (!classifOpt.get().uncertainRules().isEmpty()) {
            findings.add(classifOpt.get().uncertainRules().size() + " regles necessitent un arbitrage humain");
        }
        if (generationOpt.isEmpty()) {
            findings.add("Generation d'artefacts non disponible pour cette session");
        } else if (!generationOpt.get().warnings().isEmpty()) {
            findings.add("Generation realisee avec avertissements : " + String.join(", ", generationOpt.get().warnings()));
        }
        return List.copyOf(findings);
    }

    private List<String> buildUnknowns(final Optional<ControllerCartography> cartographyOpt) {
        var unknowns = new java.util.ArrayList<String>();

        if (cartographyOpt.isEmpty()) {
            unknowns.add("Cartographie non disponible pour cette session");
            return List.copyOf(unknowns);
        }
        for (CartographyUnknown unknown : cartographyOpt.get().unknowns()) {
            unknowns.add(unknown.location() + " : " + unknown.reason());
        }
        return List.copyOf(unknowns);
    }

    private List<String> buildLotSummaries(final Optional<MigrationPlan> migrationPlanOpt) {
        var summaries = new java.util.ArrayList<String>();

        if (migrationPlanOpt.isEmpty()) {
            summaries.add("Plan de migration non disponible");
            return List.copyOf(summaries);
        }
        for (PlannedLot lot : migrationPlanOpt.get().lots()) {
            summaries.add(formatLotSummary(lot));
        }
        return List.copyOf(summaries);
    }

    private List<String> buildArtifactSummaries(final Optional<GenerationResult> generationOpt) {
        var summaries = new java.util.ArrayList<String>();

        if (generationOpt.isEmpty()) {
            summaries.add("Aucun artefact persiste pour cette session");
            return List.copyOf(summaries);
        }
        for (CodeArtifact artifact : generationOpt.get().artifacts()) {
            summaries.add(formatArtifactSummary(artifact));
        }
        return List.copyOf(summaries);
    }

    private static String formatLotSummary(final PlannedLot lot) {
        StringBuilder summary = new StringBuilder();

        summary.append("Lot ").append(lot.lotNumber()).append(" - ").append(lot.title());
        summary.append(" : ").append(lot.objective());
        if (!lot.extractionCandidates().isEmpty()) {
            summary.append(" | candidats=").append(String.join(", ", lot.extractionCandidates()));
        }
        if (!lot.risks().isEmpty()) {
            summary.append(" | risques=").append(lot.risks().stream()
                    .map(ProduceRestitutionService::formatRisk)
                    .reduce((left, right) -> left + "; " + right)
                    .orElse(""));
        }
        return summary.toString();
    }

    private static String formatRisk(final RegressionRisk risk) {
        return risk.level() + ": " + risk.description();
    }

    private static String formatArtifactSummary(final CodeArtifact artifact) {
        StringBuilder summary = new StringBuilder();

        summary.append("Lot ").append(artifact.lotNumber());
        summary.append(" - ").append(artifact.type()).append(" - ").append(artifact.className());
        if (artifact.transitionalBridge()) {
            summary.append(" (bridge transitoire)");
        }
        if (!artifact.generationWarnings().isEmpty()) {
            summary.append(" | warnings=").append(artifact.generationWarnings().size());
        }
        return summary.toString();
    }
}
