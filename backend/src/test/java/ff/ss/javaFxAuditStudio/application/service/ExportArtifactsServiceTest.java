package ff.ss.javaFxAuditStudio.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ff.ss.javaFxAuditStudio.application.ports.out.ArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.ExportResult;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;

@ExtendWith(MockitoExtension.class)
class ExportArtifactsServiceTest {

    @Mock
    private ArtifactPersistencePort artifactPersistencePort;

    @TempDir
    Path tempDir;

    @Test
    void should_export_main_and_test_artifacts_in_maven_layout_from_project_root() throws Exception {
        ExportArtifactsService service = new ExportArtifactsService(artifactPersistencePort);
        Path projectRoot = tempDir.resolve("export-root");
        when(artifactPersistencePort.findBySessionId("sess-1"))
                .thenReturn(Optional.of(new GenerationResult(
                        "Controller.java",
                        List.of(
                                artifact(
                                        "a-1",
                                        ArtifactType.USE_CASE,
                                        "PatientUseCase",
                                        "package ff.example.application;\npublic interface PatientUseCase {}"),
                                artifact(
                                        "a-2",
                                        ArtifactType.TEST_SKELETON,
                                        "PatientUseCaseTest",
                                        "package ff.example.application;\nclass PatientUseCaseTest {}")),
                        List.of())));

        ExportResult result = service.export("sess-1", projectRoot.toString());

        Path mainFile = projectRoot.resolve("src/main/java/ff/example/application/PatientUseCase.java");
        Path testFile = projectRoot.resolve("src/test/java/ff/example/application/PatientUseCaseTest.java");
        assertThat(result.errors()).isEmpty();
        assertThat(result.exportedFiles()).contains(mainFile.toString(), testFile.toString());
        assertThat(Files.exists(mainFile)).isTrue();
        assertThat(Files.exists(testFile)).isTrue();
    }

    @Test
    void should_switch_to_test_source_root_when_target_already_points_to_main_sources() {
        ExportArtifactsService service = new ExportArtifactsService(artifactPersistencePort);
        Path mainSourceRoot = tempDir.resolve("target-project").resolve("src/main/java");
        when(artifactPersistencePort.findBySessionId("sess-2"))
                .thenReturn(Optional.of(new GenerationResult(
                        "Controller.java",
                        List.of(artifact(
                                "a-3",
                                ArtifactType.TEST_SKELETON,
                                "PatientPolicyTest",
                                "package ff.example.policy;\nclass PatientPolicyTest {}")),
                        List.of())));

        ExportResult result = service.export("sess-2", mainSourceRoot.toString());

        assertThat(result.errors()).isEmpty();
        assertThat(result.exportedFiles())
                .contains(tempDir.resolve("target-project")
                        .resolve("src/test/java/ff/example/policy/PatientPolicyTest.java")
                        .toString());
    }

    private CodeArtifact artifact(
            final String artifactId,
            final ArtifactType artifactType,
            final String className,
            final String content) {
        return new CodeArtifact(
                artifactId,
                artifactType,
                1,
                className,
                content,
                false);
    }
}
