package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGeneratedArtifactRepository extends JpaRepository<AiGeneratedArtifactEntity, String> {

    List<AiGeneratedArtifactEntity> findBySessionIdOrderByArtifactTypeAscVersionNumberDesc(String sessionId);

    List<AiGeneratedArtifactEntity> findBySessionIdAndArtifactTypeOrderByVersionNumberDesc(
            String sessionId,
            String artifactType);

    Optional<AiGeneratedArtifactEntity> findFirstBySessionIdAndArtifactTypeOrderByVersionNumberDesc(
            String sessionId,
            String artifactType);
}
