package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;

public interface GenerateArtifactsUseCase {

    GenerationResult handle(String controllerRef);
}
