package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ParsingMode;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
        ClassificationResultEntity entity = new ClassificationResultEntity(
                sessionId,
                result.controllerRef(),
                Instant.now(),
                result.parsingMode(),
                result.parsingFallbackReason());
        result.rules().forEach(rule -> entity.getRules().add(toRuleEntity(rule, entity)));
        result.uncertainRules().forEach(rule -> entity.getRules().add(toRuleEntity(rule, entity)));
        ClassificationResultEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClassificationResult> findBySessionId(final String sessionId) {
        return repository.findBySessionId(sessionId).map(this::toDomain);
    }

    private BusinessRuleEntity toRuleEntity(final BusinessRule rule, final ClassificationResultEntity parent) {
        return new BusinessRuleEntity(
                parent,
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

        ParsingMode parsingMode = entity.getParsingMode() != null
                ? entity.getParsingMode()
                : ParsingMode.AST;

        return new ClassificationResult(
                entity.getControllerRef(),
                certain,
                uncertain,
                parsingMode,
                entity.getParsingFallbackReason());
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
