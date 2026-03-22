package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.domain.rules.ReclassificationAuditEntry;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Adaptateur JPA pour la persistence des entrees d'audit de reclassification.
 */
@Component
public class JpaReclassificationAuditAdapter implements ReclassificationAuditPort {

    private final RuleClassificationAuditRepository repository;

    public JpaReclassificationAuditAdapter(final RuleClassificationAuditRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public ReclassificationAuditEntry save(final ReclassificationAuditEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        RuleClassificationAuditEntity saved = repository.save(toEntity(entry));
        return toDomain(saved);
    }

    @Override
    public List<ReclassificationAuditEntry> findByAnalysisIdAndRuleId(
            final String analysisId,
            final String ruleId) {
        return repository
                .findByAnalysisIdAndRuleIdOrderByCreatedAtAsc(analysisId, ruleId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private RuleClassificationAuditEntity toEntity(final ReclassificationAuditEntry entry) {
        return new RuleClassificationAuditEntity(
                entry.auditId(),
                entry.analysisId(),
                entry.ruleId(),
                entry.fromCategory().name(),
                entry.toCategory().name(),
                entry.reason(),
                entry.timestamp());
    }

    private ReclassificationAuditEntry toDomain(final RuleClassificationAuditEntity entity) {
        return new ReclassificationAuditEntry(
                entity.getId(),
                entity.getAnalysisId(),
                entity.getRuleId(),
                ResponsibilityClass.valueOf(entity.getFromCategory()),
                ResponsibilityClass.valueOf(entity.getToCategory()),
                entity.getReason(),
                entity.getCreatedAt());
    }
}
