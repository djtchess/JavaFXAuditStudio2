package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.application.ports.out.RestitutionPersistencePort;
import ff.ss.javaFxAuditStudio.domain.restitution.ConfidenceLevel;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionSummary;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
public class JpaRestitutionPersistenceAdapter implements RestitutionPersistencePort {

    private final RestitutionReportRepository repository;

    public JpaRestitutionPersistenceAdapter(final RestitutionReportRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public RestitutionReport save(final String sessionId, final RestitutionReport report) {
        repository.deleteBySessionId(sessionId);
        RestitutionReportEntity entity = toEntity(sessionId, report);
        RestitutionReportEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RestitutionReport> findBySessionId(final String sessionId) {
        return repository.findBySessionId(sessionId).map(this::toDomain);
    }

    private RestitutionReportEntity toEntity(final String sessionId, final RestitutionReport report) {
        RestitutionSummary s = report.summary();
        return new RestitutionReportEntity(
                sessionId,
                s.controllerRef(),
                s.ruleCount(),
                s.uncertainCount(),
                s.artifactCount(),
                s.bridgeCount(),
                s.confidence().name(),
                s.hasContradictions(),
                report.contradictions(),
                report.unknowns(),
                report.findings(),
                report.lotSummaries(),
                report.artifactSummaries(),
                report.markdown(),
                Instant.now());
    }

    private RestitutionReport toDomain(final RestitutionReportEntity e) {
        RestitutionSummary summary = new RestitutionSummary(
                e.getControllerRef(),
                e.getRuleCount(),
                e.getUncertainCount(),
                e.getArtifactCount(),
                e.getBridgeCount(),
                ConfidenceLevel.valueOf(e.getConfidence()),
                e.isHasContradictions());

        return new RestitutionReport(
                summary,
                e.getContradictions(),
                e.getUnknowns(),
                e.getFindings(),
                e.getLotSummaries(),
                e.getArtifactSummaries(),
                e.getMarkdown());
    }
}
