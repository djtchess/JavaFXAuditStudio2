package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class JpaClassificationPersistenceAdapter implements ClassificationPersistencePort {

    private final ClassificationResultRepository repository;

    public JpaClassificationPersistenceAdapter(final ClassificationResultRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ClassificationResult save(final String sessionId, final ClassificationResult result) {
        repository.deleteBySessionId(sessionId);
        List<BusinessRuleEntity> allRuleEntities = buildRuleEntities(result);
        ClassificationResultEntity entity = new ClassificationResultEntity(
                sessionId,
                result.controllerRef(),
                Instant.now(),
                new ArrayList<>(allRuleEntities));
        ClassificationResultEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClassificationResult> findBySessionId(final String sessionId) {
        return repository.findBySessionId(sessionId).map(this::toDomain);
    }

    private List<BusinessRuleEntity> buildRuleEntities(final ClassificationResult result) {
        List<BusinessRuleEntity> entities = new ArrayList<>();
        result.rules().forEach(rule -> entities.add(toRuleEntity(rule)));
        result.uncertainRules().forEach(rule -> entities.add(toRuleEntity(rule)));
        return entities;
    }

    private BusinessRuleEntity toRuleEntity(final BusinessRule rule) {
        return new BusinessRuleEntity(
                null,
                rule.ruleId(),
                rule.description(),
                rule.sourceRef(),
                rule.sourceLine(),
                rule.responsibilityClass().name(),
                rule.extractionCandidate().name(),
                rule.uncertain());
    }

    private ClassificationResult toDomain(final ClassificationResultEntity entity) {
        List<BusinessRule> allRules = entity.getRules().stream()
                .map(this::toRuleDomain)
                .toList();

        List<BusinessRule> certain = allRules.stream().filter(r -> !r.uncertain()).toList();
        List<BusinessRule> uncertain = allRules.stream().filter(BusinessRule::uncertain).toList();

        return new ClassificationResult(entity.getControllerRef(), certain, uncertain);
    }

    private BusinessRule toRuleDomain(final BusinessRuleEntity e) {
        return new BusinessRule(
                e.getRuleId(),
                e.getDescription(),
                e.getSourceRef(),
                e.getSourceLine(),
                ResponsibilityClass.valueOf(e.getResponsibilityClass()),
                ExtractionCandidate.valueOf(e.getExtractionCandidate()),
                e.isUncertain());
    }
}
