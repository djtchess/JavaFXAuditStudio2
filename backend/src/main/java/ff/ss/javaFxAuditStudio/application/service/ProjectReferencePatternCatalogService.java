package ff.ss.javaFxAuditStudio.application.service;

import java.util.List;
import java.util.Objects;

import ff.ss.javaFxAuditStudio.application.ports.in.ListProjectReferencePatternsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.RegisterProjectReferencePatternUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ProjectReferencePatternPort;
import ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern;

public class ProjectReferencePatternCatalogService
        implements RegisterProjectReferencePatternUseCase, ListProjectReferencePatternsUseCase {

    private final ProjectReferencePatternPort projectReferencePatternPort;

    public ProjectReferencePatternCatalogService(final ProjectReferencePatternPort projectReferencePatternPort) {
        this.projectReferencePatternPort = Objects.requireNonNull(
                projectReferencePatternPort,
                "projectReferencePatternPort must not be null");
    }

    @Override
    public ProjectReferencePattern register(
            final String artifactType,
            final String referenceName,
            final String content) {
        return projectReferencePatternPort.save(artifactType, referenceName, content);
    }

    @Override
    public List<ProjectReferencePattern> list(final String artifactType) {
        List<ProjectReferencePattern> patterns = (artifactType == null || artifactType.isBlank())
                ? projectReferencePatternPort.findAll()
                : projectReferencePatternPort.findByArtifactType(artifactType);
        return List.copyOf(patterns);
    }
}
