package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectReferencePatternRepository extends JpaRepository<ProjectReferencePatternEntity, String> {

    List<ProjectReferencePatternEntity> findByArtifactTypeOrderByCreatedAtDesc(String artifactType);

    List<ProjectReferencePatternEntity> findAllByOrderByCreatedAtDesc();
}
