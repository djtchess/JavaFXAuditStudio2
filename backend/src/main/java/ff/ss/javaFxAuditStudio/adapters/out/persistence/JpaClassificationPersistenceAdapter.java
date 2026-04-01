package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.domain.analysis.ControllerDependency;
import ff.ss.javaFxAuditStudio.domain.analysis.DeltaAnalysisSummary;
import ff.ss.javaFxAuditStudio.domain.analysis.DetectionStatus;
import ff.ss.javaFxAuditStudio.domain.analysis.StateMachineInsight;
import ff.ss.javaFxAuditStudio.domain.analysis.StateTransition;
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
                result.parsingFallbackReason(),
                result.excludedLifecycleMethodsCount());
        result.rules().forEach(rule -> entity.getRules().add(toRuleEntity(rule, entity)));
        result.uncertainRules().forEach(rule -> entity.getRules().add(toRuleEntity(rule, entity)));
        entity.setStateMachineStatus(result.stateMachine().status());
        entity.setStateMachineConfidence(result.stateMachine().confidence());
        entity.getStateMachineStates().addAll(result.stateMachine().states());
        result.stateMachine().transitions().forEach(transition ->
                entity.getStateTransitions().add(new StateTransitionEmbeddable(
                        transition.fromState(),
                        transition.toState(),
                        transition.trigger())));
        result.dependencies().forEach(dependency ->
                entity.getDependencies().add(new ClassificationDependencyEmbeddable(
                        dependency.kind(),
                        dependency.target(),
                        dependency.via())));
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
        DetectionStatus detectionStatus = entity.getStateMachineStatus() != null
                ? entity.getStateMachineStatus()
                : DetectionStatus.ABSENT;
        StateMachineInsight stateMachine = new StateMachineInsight(
                detectionStatus,
                entity.getStateMachineConfidence(),
                entity.getStateMachineStates(),
                entity.getStateTransitions().stream()
                        .map(this::toStateTransition)
                        .toList());
        List<ControllerDependency> dependencies = entity.getDependencies().stream()
                .map(this::toDependencyDomain)
                .toList();

        return new ClassificationResult(
                entity.getControllerRef(),
                certain,
                uncertain,
                parsingMode,
                entity.getParsingFallbackReason(),
                entity.getExcludedLifecycleMethodsCount(),
                stateMachine,
                dependencies,
                DeltaAnalysisSummary.none());
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

    private StateTransition toStateTransition(final StateTransitionEmbeddable transition) {
        return new StateTransition(
                transition.getFromState(),
                transition.getToState(),
                transition.getTrigger());
    }

    private ControllerDependency toDependencyDomain(final ClassificationDependencyEmbeddable dependency) {
        return new ControllerDependency(
                dependency.getKind(),
                dependency.getTarget(),
                dependency.getVia());
    }
}
