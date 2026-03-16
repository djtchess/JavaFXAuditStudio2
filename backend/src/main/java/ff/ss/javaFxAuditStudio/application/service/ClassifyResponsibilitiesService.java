package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.ClassifyResponsibilitiesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ClassifyResponsibilitiesService implements ClassifyResponsibilitiesUseCase {

    private final RuleExtractionPort ruleExtractionPort;
    private final ClassificationPersistencePort classificationPersistencePort;

    public ClassifyResponsibilitiesService(
            final RuleExtractionPort ruleExtractionPort,
            final ClassificationPersistencePort classificationPersistencePort) {
        this.ruleExtractionPort = Objects.requireNonNull(
                ruleExtractionPort, "ruleExtractionPort must not be null");
        this.classificationPersistencePort = Objects.requireNonNull(
                classificationPersistencePort, "classificationPersistencePort must not be null");
    }

    /**
     * Classifie les responsabilites du controller designe.
     * Le contenu Java est transmis vide en attendant la connexion a l'ingestion (JAS future).
     * Les regles sont partitionnees entre certaines et incertaines selon {@code rule.uncertain()}.
     */
    @Override
    public ClassificationResult handle(final String sessionId, final String controllerRef) {
        Optional<ClassificationResult> cached = classificationPersistencePort.findBySessionId(sessionId);
        if (cached.isPresent()) {
            return cached.get();
        }

        List<BusinessRule> allRules = ruleExtractionPort.extract(controllerRef, "");
        List<BusinessRule> certain = allRules.stream().filter(rule -> !rule.uncertain()).toList();
        List<BusinessRule> uncertain = allRules.stream().filter(BusinessRule::uncertain).toList();
        ClassificationResult result = new ClassificationResult(controllerRef, certain, uncertain);

        classificationPersistencePort.save(sessionId, result);
        return result;
    }
}
