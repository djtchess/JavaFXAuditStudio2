package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern;

public interface RegisterProjectReferencePatternUseCase {

    ProjectReferencePattern register(String artifactType, String referenceName, String content);
}
