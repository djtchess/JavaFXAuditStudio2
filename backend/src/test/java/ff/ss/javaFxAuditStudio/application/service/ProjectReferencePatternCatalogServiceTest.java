package ff.ss.javaFxAuditStudio.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ff.ss.javaFxAuditStudio.application.ports.out.ProjectReferencePatternPort;
import ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern;

@ExtendWith(MockitoExtension.class)
class ProjectReferencePatternCatalogServiceTest {

    @Mock
    private ProjectReferencePatternPort projectReferencePatternPort;

    @Test
    void should_register_project_reference_pattern() {
        ProjectReferencePattern expected = pattern("USE_CASE", "ReferenceUseCase");
        when(projectReferencePatternPort.save("USE_CASE", "ReferenceUseCase", "code")).thenReturn(expected);

        ProjectReferencePatternCatalogService service =
                new ProjectReferencePatternCatalogService(projectReferencePatternPort);

        ProjectReferencePattern result = service.register("USE_CASE", "ReferenceUseCase", "code");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void should_list_all_patterns_when_artifact_type_is_blank() {
        when(projectReferencePatternPort.findAll()).thenReturn(List.of(pattern("USE_CASE", "ReferenceUseCase")));

        ProjectReferencePatternCatalogService service =
                new ProjectReferencePatternCatalogService(projectReferencePatternPort);

        List<ProjectReferencePattern> result = service.list(" ");

        assertThat(result).hasSize(1);
        verify(projectReferencePatternPort).findAll();
    }

    @Test
    void should_list_patterns_for_specific_artifact_type() {
        when(projectReferencePatternPort.findByArtifactType("USE_CASE"))
                .thenReturn(List.of(pattern("USE_CASE", "ReferenceUseCase")));

        ProjectReferencePatternCatalogService service =
                new ProjectReferencePatternCatalogService(projectReferencePatternPort);

        List<ProjectReferencePattern> result = service.list("USE_CASE");

        assertThat(result).hasSize(1);
        verify(projectReferencePatternPort).findByArtifactType("USE_CASE");
    }

    private static ProjectReferencePattern pattern(final String artifactType, final String referenceName) {
        return new ProjectReferencePattern(
                artifactType + "-1",
                artifactType,
                referenceName,
                "package ff.example;\npublic interface " + referenceName + " {}",
                Instant.now());
    }
}
