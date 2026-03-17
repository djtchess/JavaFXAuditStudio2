package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.ClassifyResponsibilitiesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceReaderPort;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionResult;
import ff.ss.javaFxAuditStudio.domain.rules.ParsingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ClassifyResponsibilitiesService implements ClassifyResponsibilitiesUseCase {

    private static final Logger log = LoggerFactory.getLogger(ClassifyResponsibilitiesService.class);

    private final RuleExtractionPort ruleExtractionPort;
    private final ClassificationPersistencePort classificationPersistencePort;
    private final SourceReaderPort sourceReaderPort;

    public ClassifyResponsibilitiesService(
            final RuleExtractionPort ruleExtractionPort,
            final ClassificationPersistencePort classificationPersistencePort,
            final SourceReaderPort sourceReaderPort) {
        this.ruleExtractionPort = Objects.requireNonNull(ruleExtractionPort);
        this.classificationPersistencePort = Objects.requireNonNull(classificationPersistencePort);
        this.sourceReaderPort = Objects.requireNonNull(sourceReaderPort);
    }

    @Override
    public ClassificationResult handle(final String sessionId, final String controllerRef) {
        Optional<ClassificationResult> cached = classificationPersistencePort.findBySessionId(sessionId);
        if (cached.isPresent()) {
            return cached.get();
        }

        String javaContent = readJavaContent(controllerRef);

        ExtractionResult extraction = ruleExtractionPort.extract(controllerRef, javaContent);
        List<BusinessRule> allRules = extraction.rules();
        ParsingMode parsingMode = extraction.parsingMode();
        String fallbackReason = extraction.fallbackReason();

        List<BusinessRule> certain = allRules.stream().filter(rule -> !rule.uncertain()).toList();
        List<BusinessRule> uncertain = allRules.stream().filter(BusinessRule::uncertain).toList();
        ClassificationResult result = new ClassificationResult(
                controllerRef, certain, uncertain, parsingMode, fallbackReason);

        classificationPersistencePort.save(sessionId, result);

        log.debug("Classification terminee - {} regles certaines, {} incertaines, mode={}",
                certain.size(), uncertain.size(), parsingMode);
        return result;
    }

    private String readJavaContent(final String controllerRef) {
        if (controllerRef == null || controllerRef.isBlank()) {
            return "";
        }
        return sourceReaderPort.read(controllerRef)
                .map(input -> input.content())
                .orElseGet(() -> {
                    log.warn("Controller introuvable pour la classification - ref={}", controllerRef);
                    return "";
                });
    }
}
