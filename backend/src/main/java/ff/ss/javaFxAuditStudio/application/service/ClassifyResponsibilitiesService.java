package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.ClassifyResponsibilitiesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;

import java.util.List;
import java.util.Objects;

public final class ClassifyResponsibilitiesService implements ClassifyResponsibilitiesUseCase {

    private final RuleExtractionPort ruleExtractionPort;

    public ClassifyResponsibilitiesService(final RuleExtractionPort ruleExtractionPort) {
        this.ruleExtractionPort = Objects.requireNonNull(
                ruleExtractionPort, "ruleExtractionPort must not be null");
    }

    /**
     * Classifie les responsabilités du controller désigné.
     * Le contenu Java est transmis vide en attendant la connexion à l'ingestion (JAS future).
     * Les règles sont partitionnées entre certaines et incertaines selon {@code rule.uncertain()}.
     */
    @Override
    public ClassificationResult handle(final String controllerRef) {
        List<BusinessRule> allRules;
        List<BusinessRule> certain;
        List<BusinessRule> uncertain;

        allRules = ruleExtractionPort.extract(controllerRef, "");
        certain = allRules.stream().filter(rule -> !rule.uncertain()).toList();
        uncertain = allRules.stream().filter(BusinessRule::uncertain).toList();
        return new ClassificationResult(controllerRef, certain, uncertain);
    }
}
