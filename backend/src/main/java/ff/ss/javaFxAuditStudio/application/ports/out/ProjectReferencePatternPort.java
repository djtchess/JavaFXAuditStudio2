package ff.ss.javaFxAuditStudio.application.ports.out;

import java.util.List;

import ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern;

public interface ProjectReferencePatternPort {

    ProjectReferencePattern save(String artifactType, String referenceName, String content);

    List<ProjectReferencePattern> findByArtifactType(String artifactType);

    List<ProjectReferencePattern> findAll();
}
