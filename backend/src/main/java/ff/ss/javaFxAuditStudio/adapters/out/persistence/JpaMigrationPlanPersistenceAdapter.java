package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.application.ports.out.MigrationPlanPersistencePort;
import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;
import ff.ss.javaFxAuditStudio.domain.migration.PlannedLot;
import ff.ss.javaFxAuditStudio.domain.migration.RegressionRisk;
import ff.ss.javaFxAuditStudio.domain.migration.RiskLevel;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class JpaMigrationPlanPersistenceAdapter implements MigrationPlanPersistencePort {

    private final MigrationPlanRepository repository;

    public JpaMigrationPlanPersistenceAdapter(final MigrationPlanRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public MigrationPlan save(final String sessionId, final MigrationPlan plan) {
        repository.deleteBySessionId(sessionId);
        MigrationPlanEntity entity = toEntity(sessionId, plan);
        MigrationPlanEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MigrationPlan> findBySessionId(final String sessionId) {
        return repository.findBySessionId(sessionId).map(this::toDomain);
    }

    private MigrationPlanEntity toEntity(final String sessionId, final MigrationPlan plan) {
        MigrationPlanEntity entity = new MigrationPlanEntity(
                sessionId, plan.controllerRef(), plan.compilable(), Instant.now());
        plan.lots().stream()
                .map(lot -> toLotEntity(lot, entity))
                .forEach(entity.getLots()::add);
        return entity;
    }

    private PlannedLotEntity toLotEntity(final PlannedLot lot, final MigrationPlanEntity plan) {
        PlannedLotEntity lotEntity = new PlannedLotEntity(
                plan, lot.lotNumber(), lot.title(), lot.objective(), lot.extractionCandidates());
        lot.risks().stream()
                .map(r -> new RegressionRiskEntity(lotEntity, r.description(), r.level().name(), r.mitigation()))
                .forEach(lotEntity.getRisks()::add);
        return lotEntity;
    }

    private MigrationPlan toDomain(final MigrationPlanEntity entity) {
        List<PlannedLot> lots = entity.getLots().stream()
                .map(this::toLotDomain)
                .toList();
        return new MigrationPlan(entity.getControllerRef(), lots, entity.isCompilable());
    }

    private PlannedLot toLotDomain(final PlannedLotEntity e) {
        List<RegressionRisk> risks = e.getRisks().stream()
                .map(r -> new RegressionRisk(r.getDescription(), RiskLevel.valueOf(r.getRiskLevel()), r.getMitigation()))
                .toList();

        return new PlannedLot(
                e.getLotNumber(),
                e.getTitle(),
                e.getObjective(),
                e.getExtractionCandidates(),
                risks);
    }
}
