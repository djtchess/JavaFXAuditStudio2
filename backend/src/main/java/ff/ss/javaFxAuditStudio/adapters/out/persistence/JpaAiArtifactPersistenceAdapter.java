package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ff.ss.javaFxAuditStudio.application.ports.out.AiArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.domain.ai.AiGeneratedArtifact;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;

/**
 * Adaptateur JPA pour les artefacts IA versionnés.
 */
@Component
public class JpaAiArtifactPersistenceAdapter implements AiArtifactPersistencePort {

    private static final Pattern TYPE_DECLARATION = Pattern.compile(
            "\\b(?:class|interface|record)\\s+([A-Za-z_][A-Za-z0-9_]*)");

    private final AiGeneratedArtifactRepository repository;

    public JpaAiArtifactPersistenceAdapter(final AiGeneratedArtifactRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    @Transactional
    public List<AiGeneratedArtifact> saveGeneratedArtifacts(
            final String sessionId,
            final String requestId,
            final LlmProvider provider,
            final TaskType originTask,
            final Map<String, String> generatedArtifacts) {
        Objects.requireNonNull(generatedArtifacts, "generatedArtifacts must not be null");

        List<AiGeneratedArtifact> saved = new ArrayList<>();
        for (Map.Entry<String, String> entry : generatedArtifacts.entrySet()) {
            saved.add(saveArtifactVersion(
                    sessionId,
                    entry.getKey(),
                    entry.getValue(),
                    requestId,
                    provider,
                    originTask));
        }
        return List.copyOf(saved);
    }

    @Override
    @Transactional
    public AiGeneratedArtifact saveArtifactVersion(
            final String sessionId,
            final String artifactType,
            final String content,
            final String requestId,
            final LlmProvider provider,
            final TaskType originTask) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(artifactType, "artifactType must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(originTask, "originTask must not be null");

        String normalizedArtifactType = normalizeArtifactType(artifactType);
        Optional<AiGeneratedArtifactEntity> latestEntity = repository
                .findFirstBySessionIdAndArtifactTypeOrderByVersionNumberDesc(sessionId, normalizedArtifactType);
        int versionNumber = latestEntity.map(entity -> entity.getVersionNumber() + 1).orElse(1);
        String className = resolveClassName(normalizedArtifactType, content, latestEntity);
        AiGeneratedArtifactEntity entity = new AiGeneratedArtifactEntity(
                UUID.randomUUID().toString(),
                sessionId,
                normalizedArtifactType,
                className,
                normalizeContent(content),
                versionNumber,
                latestEntity.map(AiGeneratedArtifactEntity::getId).orElse(null),
                requestId,
                provider.value(),
                originTask.name(),
                Instant.now());

        return toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGeneratedArtifact> findLatestBySessionId(final String sessionId) {
        List<AiGeneratedArtifactEntity> entities = repository.findBySessionIdOrderByArtifactTypeAscVersionNumberDesc(sessionId);
        Map<String, AiGeneratedArtifact> latestByType = new LinkedHashMap<>();
        for (AiGeneratedArtifactEntity entity : entities) {
            latestByType.putIfAbsent(entity.getArtifactType(), toDomain(entity));
        }
        return List.copyOf(latestByType.values());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGeneratedArtifact> findVersions(final String sessionId, final String artifactType) {
        return repository.findBySessionIdAndArtifactTypeOrderByVersionNumberDesc(
                        sessionId,
                        artifactType.trim().toUpperCase())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiGeneratedArtifact> findLatestBySessionIdAndArtifactType(
            final String sessionId,
            final String artifactType) {
        return repository.findFirstBySessionIdAndArtifactTypeOrderByVersionNumberDesc(
                        sessionId,
                        artifactType.trim().toUpperCase())
                .map(this::toDomain);
    }

    private String normalizeArtifactType(final String artifactType) {
        String normalized = artifactType.trim().toUpperCase();
        ArtifactType.valueOf(normalized);
        return normalized;
    }

    private String resolveClassName(
            final String artifactType,
            final String content,
            final Optional<AiGeneratedArtifactEntity> latestEntity) {
        String className = extractTypeName(content);
        if (className == null && latestEntity.isPresent()) {
            className = latestEntity.get().getClassName();
        }
        if (className == null) {
            className = artifactType.trim().toUpperCase() + "Generated";
        }
        return className;
    }

    private String extractTypeName(final String content) {
        Matcher matcher = TYPE_DECLARATION.matcher(content);
        String typeName = null;
        if (matcher.find()) {
            typeName = matcher.group(1);
        }
        return typeName;
    }

    private String normalizeContent(final String content) {
        return content.replace("\r\n", "\n").replace("\r", "\n");
    }

    private AiGeneratedArtifact toDomain(final AiGeneratedArtifactEntity entity) {
        return new AiGeneratedArtifact(
                entity.getId(),
                entity.getSessionId(),
                entity.getArtifactType(),
                entity.getClassName(),
                entity.getContent(),
                entity.getVersionNumber(),
                entity.getParentVersionId(),
                entity.getRequestId(),
                LlmProvider.fromString(entity.getProvider()),
                TaskType.valueOf(entity.getOriginTask()),
                entity.getCreatedAt());
    }
}
