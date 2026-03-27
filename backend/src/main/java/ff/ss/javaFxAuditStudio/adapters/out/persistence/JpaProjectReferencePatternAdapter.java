package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ff.ss.javaFxAuditStudio.application.ports.out.ProjectReferencePatternPort;
import ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;

@Component
public class JpaProjectReferencePatternAdapter implements ProjectReferencePatternPort {

    private final ProjectReferencePatternRepository repository;

    public JpaProjectReferencePatternAdapter(final ProjectReferencePatternRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    @Transactional
    public ProjectReferencePattern save(
            final String artifactType,
            final String referenceName,
            final String content) {
        String normalizedArtifactType = normalizeArtifactType(artifactType);
        ProjectReferencePatternEntity entity = new ProjectReferencePatternEntity(
                UUID.randomUUID().toString(),
                normalizedArtifactType,
                referenceName.trim(),
                content.replace("\r\n", "\n").replace("\r", "\n"),
                Instant.now());
        return toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectReferencePattern> findByArtifactType(final String artifactType) {
        return repository.findByArtifactTypeOrderByCreatedAtDesc(normalizeArtifactType(artifactType))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectReferencePattern> findAll() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private String normalizeArtifactType(final String artifactType) {
        String normalized = artifactType.trim().toUpperCase();
        ArtifactType.valueOf(normalized);
        return normalized;
    }

    private ProjectReferencePattern toDomain(final ProjectReferencePatternEntity entity) {
        return new ProjectReferencePattern(
                entity.getId(),
                entity.getArtifactType(),
                entity.getReferenceName(),
                entity.getContent(),
                entity.getCreatedAt());
    }
}
