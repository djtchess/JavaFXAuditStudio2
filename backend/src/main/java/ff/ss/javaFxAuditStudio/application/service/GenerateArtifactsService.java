package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.CodeGenerationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceReaderPort;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GenerateArtifactsService implements GenerateArtifactsUseCase {

    private static final Logger log = LoggerFactory.getLogger(GenerateArtifactsService.class);

    private final CodeGenerationPort codeGenerationPort;
    private final ArtifactPersistencePort artifactPersistencePort;
    private final ClassificationPersistencePort classificationPersistencePort;
    private final SourceReaderPort sourceReaderPort;

    public GenerateArtifactsService(
            final CodeGenerationPort codeGenerationPort,
            final ArtifactPersistencePort artifactPersistencePort,
            final ClassificationPersistencePort classificationPersistencePort,
            final SourceReaderPort sourceReaderPort) {
        this.codeGenerationPort = Objects.requireNonNull(codeGenerationPort);
        this.artifactPersistencePort = Objects.requireNonNull(artifactPersistencePort);
        this.classificationPersistencePort = Objects.requireNonNull(classificationPersistencePort);
        this.sourceReaderPort = Objects.requireNonNull(sourceReaderPort);
    }

    @Override
    public GenerationResult handle(final String sessionId, final String controllerRef) {
        Optional<GenerationResult> cached = artifactPersistencePort.findBySessionId(sessionId);
        if (cached.isPresent()) {
            return cached.get();
        }

        List<BusinessRule> allRules = classificationPersistencePort.findBySessionId(sessionId)
                .map(r -> {
                    var combined = new java.util.ArrayList<>(r.rules());
                    combined.addAll(r.uncertainRules());
                    return (List<BusinessRule>) combined;
                })
                .orElse(List.of());

        String javaContent = sourceReaderPort.read(controllerRef)
                .map(input -> input.content())
                .orElseGet(() -> {
                    log.warn("Source introuvable pour la generation - ref={}", controllerRef);
                    return "";
                });

        final List<CodeArtifact> artifacts = codeGenerationPort.generate(controllerRef, javaContent, allRules);
        GenerationResult result = new GenerationResult(controllerRef, artifacts, List.of());

        artifactPersistencePort.save(sessionId, result);
        log.debug("Generation terminee - {} artefacts", artifacts.size());
        return result;
    }
}
