package ff.ss.javaFxAuditStudio.application.ports.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import ff.ss.javaFxAuditStudio.domain.ai.AiGeneratedArtifact;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;

/**
 * Port sortant pour la persistence versionnée des artefacts générés par l'IA.
 */
public interface AiArtifactPersistencePort {

    List<AiGeneratedArtifact> saveGeneratedArtifacts(
            String sessionId,
            String requestId,
            LlmProvider provider,
            TaskType originTask,
            Map<String, String> generatedArtifacts);

    AiGeneratedArtifact saveArtifactVersion(
            String sessionId,
            String artifactType,
            String content,
            String requestId,
            LlmProvider provider,
            TaskType originTask);

    List<AiGeneratedArtifact> findLatestBySessionId(String sessionId);

    List<AiGeneratedArtifact> findVersions(String sessionId, String artifactType);

    Optional<AiGeneratedArtifact> findLatestBySessionIdAndArtifactType(String sessionId, String artifactType);
}
