package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.CodeGenerationPort;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GenerateArtifactsService implements GenerateArtifactsUseCase {

    private static final String STUB_WARNING = "Generation stub - contenu source non connecte";

    private final CodeGenerationPort codeGenerationPort;
    private final ArtifactPersistencePort artifactPersistencePort;

    public GenerateArtifactsService(
            final CodeGenerationPort codeGenerationPort,
            final ArtifactPersistencePort artifactPersistencePort) {
        this.codeGenerationPort = Objects.requireNonNull(
                codeGenerationPort, "codeGenerationPort must not be null");
        this.artifactPersistencePort = Objects.requireNonNull(
                artifactPersistencePort, "artifactPersistencePort must not be null");
    }

    @Override
    public GenerationResult handle(final String sessionId, final String controllerRef) {
        Optional<GenerationResult> cached = artifactPersistencePort.findBySessionId(sessionId);
        if (cached.isPresent()) {
            return cached.get();
        }

        final List<CodeArtifact> artifacts = codeGenerationPort.generate(controllerRef, "");
        final List<String> warnings = buildWarnings(artifacts);
        GenerationResult result = new GenerationResult(controllerRef, artifacts, warnings);

        artifactPersistencePort.save(sessionId, result);
        return result;
    }

    private List<String> buildWarnings(final List<CodeArtifact> artifacts) {
        final List<String> warnings = new ArrayList<>();
        if (artifacts.isEmpty()) {
            warnings.add(STUB_WARNING);
        }
        return warnings;
    }
}
