package ff.ss.javaFxAuditStudio.application.ports.in;

import java.util.List;

import ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern;

public interface ListProjectReferencePatternsUseCase {

    List<ProjectReferencePattern> list(String artifactType);
}
