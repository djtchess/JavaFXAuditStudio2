package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodeArtifactRepository extends JpaRepository<CodeArtifactEntity, Long> {

    List<CodeArtifactEntity> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
