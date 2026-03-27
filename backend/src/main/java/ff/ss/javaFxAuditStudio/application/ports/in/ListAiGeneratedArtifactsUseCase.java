package ff.ss.javaFxAuditStudio.application.ports.in;

import java.util.List;

import ff.ss.javaFxAuditStudio.domain.ai.AiGeneratedArtifact;

/**
 * Consultation des artefacts IA persistés.
 */
public interface ListAiGeneratedArtifactsUseCase {

    List<AiGeneratedArtifact> listLatest(String sessionId);

    List<AiGeneratedArtifact> listVersions(String sessionId, String artifactType);
}
