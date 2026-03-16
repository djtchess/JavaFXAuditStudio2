package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.CodeGenerationPort;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GenerateArtifactsService implements GenerateArtifactsUseCase {

    private static final String STUB_WARNING = "Génération stub - contenu source non connecté";

    private final CodeGenerationPort codeGenerationPort;

    public GenerateArtifactsService(final CodeGenerationPort codeGenerationPort) {
        Objects.requireNonNull(codeGenerationPort, "codeGenerationPort must not be null");
        this.codeGenerationPort = codeGenerationPort;
    }

    @Override
    public GenerationResult handle(final String controllerRef) {
        final List<CodeArtifact> artifacts = codeGenerationPort.generate(controllerRef, "");
        final List<String> warnings = buildWarnings(artifacts);
        return new GenerationResult(controllerRef, artifacts, warnings);
    }

    private List<String> buildWarnings(final List<CodeArtifact> artifacts) {
        final List<String> warnings = new ArrayList<>();
        if (artifacts.isEmpty()) {
            warnings.add(STUB_WARNING);
        }
        return warnings;
    }
}
