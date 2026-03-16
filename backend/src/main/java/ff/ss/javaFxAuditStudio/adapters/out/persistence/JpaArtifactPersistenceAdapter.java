package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.application.ports.out.ArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adapter JPA pour la persistence des resultats de generation.
 * Les artefacts sont stockes a plat (pas de parent entity) car chaque artefact
 * porte directement le sessionId. Les warnings ne sont pas persistes car ils
 * sont recalcules a partir de la liste d'artefacts.
 */
@Component
public class JpaArtifactPersistenceAdapter implements ArtifactPersistencePort {

    private final CodeArtifactRepository repository;

    public JpaArtifactPersistenceAdapter(final CodeArtifactRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public GenerationResult save(final String sessionId, final GenerationResult result) {
        repository.deleteBySessionId(sessionId);
        List<CodeArtifactEntity> entities = result.artifacts().stream()
                .map(a -> toEntity(sessionId, result.controllerRef(), a))
                .toList();
        repository.saveAll(entities);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GenerationResult> findBySessionId(final String sessionId) {
        List<CodeArtifactEntity> entities = repository.findBySessionId(sessionId);
        if (entities.isEmpty()) {
            return Optional.empty();
        }
        List<CodeArtifact> artifacts = entities.stream()
                .map(this::toDomain)
                .toList();
        String controllerRef = extractControllerRef(entities);
        return Optional.of(new GenerationResult(controllerRef, artifacts, List.of()));
    }

    private CodeArtifactEntity toEntity(final String sessionId, final String controllerRef, final CodeArtifact a) {
        return new CodeArtifactEntity(
                sessionId,
                controllerRef,
                a.artifactId(),
                a.type().name(),
                a.lotNumber(),
                a.className(),
                a.content(),
                a.transitionalBridge(),
                Instant.now());
    }

    private CodeArtifact toDomain(final CodeArtifactEntity e) {
        return new CodeArtifact(
                e.getArtifactId(),
                ArtifactType.valueOf(e.getArtifactType()),
                e.getLotNumber(),
                e.getClassName(),
                e.getContent(),
                e.isTransitionalBridge());
    }

    private String extractControllerRef(final List<CodeArtifactEntity> entities) {
        return entities.get(0).getControllerRef();
    }
}
