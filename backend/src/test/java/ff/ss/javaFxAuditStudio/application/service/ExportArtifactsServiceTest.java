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

import ff.ss.javaFxAuditStudio.application.ports.out.AiArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.domain.ai.AiGeneratedArtifact;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.ExportResult;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class ExportArtifactsServiceTest {

    @Mock
    private ArtifactPersistencePort artifactPersistencePort;

    @Mock
    private AiArtifactPersistencePort aiArtifactPersistencePort;

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

    @Test
    void should_export_ai_generated_artifacts_instead_of_template_when_available() throws Exception {
        ExportArtifactsService service = new ExportArtifactsService(artifactPersistencePort, aiArtifactPersistencePort);
        Path projectRoot = tempDir.resolve("export-ai-root");
        when(artifactPersistencePort.findBySessionId("sess-ai"))
                .thenReturn(Optional.of(new GenerationResult(
                        "Controller.java",
                        List.of(
                                artifact(
                                        "a-1",
                                        ArtifactType.USE_CASE,
                                        "PatientUseCase",
                                        "package ff.example.application;\npublic interface PatientUseCase { void template(); }"),
                                artifact(
                                        "a-2",
                                        ArtifactType.TEST_SKELETON,
                                        "PatientUseCaseTest",
                                        "package ff.example.application;\nclass PatientUseCaseTest {}")),
                        List.of())));
        when(aiArtifactPersistencePort.findLatestBySessionId("sess-ai"))
                .thenReturn(List.of(aiArtifact(
                        "sess-ai",
                        "USE_CASE",
                        "PatientUseCase",
                        "package ff.example.application;\npublic interface PatientUseCase {\n    void execute();\n}")));

        ExportResult result = service.export("sess-ai", projectRoot.toString());

        Path mainFile = projectRoot.resolve("src/main/java/ff/example/application/PatientUseCase.java");
        Path testFile = projectRoot.resolve("src/test/java/ff/example/application/PatientUseCaseTest.java");
        assertThat(result.errors()).isEmpty();
        assertThat(Files.readString(mainFile)).contains("void execute();").doesNotContain("void template();");
        assertThat(Files.exists(testFile)).isTrue();
    }

    @Test
    void should_export_ai_generated_artifacts_even_without_template_generation() throws Exception {
        ExportArtifactsService service = new ExportArtifactsService(artifactPersistencePort, aiArtifactPersistencePort);
        Path projectRoot = tempDir.resolve("export-ai-only-root");
        when(artifactPersistencePort.findBySessionId("sess-ai-only")).thenReturn(Optional.empty());
        when(aiArtifactPersistencePort.findLatestBySessionId("sess-ai-only"))
                .thenReturn(List.of(aiArtifact(
                        "sess-ai-only",
                        "VIEW_MODEL",
                        "PatientViewModel",
                        "package ff.example.viewmodel;\npublic class PatientViewModel {}")));

        ExportResult result = service.export("sess-ai-only", projectRoot.toString());

        Path mainFile = projectRoot.resolve("src/main/java/ff/example/viewmodel/PatientViewModel.java");
        assertThat(result.errors()).isEmpty();
        assertThat(result.exportedFiles()).contains(mainFile.toString());
        assertThat(Files.exists(mainFile)).isTrue();
    }

    @Test
    void should_fallback_to_template_when_ai_artifact_is_incomplete() throws Exception {
        ExportArtifactsService service = new ExportArtifactsService(artifactPersistencePort, aiArtifactPersistencePort);
        Path projectRoot = tempDir.resolve("export-ai-fallback-root");
        when(artifactPersistencePort.findBySessionId("sess-ai-fallback"))
                .thenReturn(Optional.of(new GenerationResult(
                        "Controller.java",
                        List.of(artifact(
                                "a-1",
                                ArtifactType.POLICY,
                                "PatientPolicy",
                                "package ff.example.policy;\npublic class PatientPolicy {\n    boolean isReady() {\n        return true;\n    }\n}")),
                        List.of())));
        when(aiArtifactPersistencePort.findLatestBySessionId("sess-ai-fallback"))
                .thenReturn(List.of(aiArtifact(
                        "sess-ai-fallback",
                        "POLICY",
                        "PatientPolicy",
                        "package ff.example.policy;\npublic class PatientPolicy {\n    // TODO: implementer\n    boolean isReady() {\n        return false;\n    }\n}")));

        ExportResult result = service.export("sess-ai-fallback", projectRoot.toString());

        Path mainFile = projectRoot.resolve("src/main/java/ff/example/policy/PatientPolicy.java");
        assertThat(result.errors()).isEmpty();
        assertThat(Files.readString(mainFile)).contains("return true;").doesNotContain("// TODO: implementer");
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

    private AiGeneratedArtifact aiArtifact(
            final String sessionId,
            final String artifactType,
            final String className,
            final String content) {
        return new AiGeneratedArtifact(
                "ai-" + artifactType,
                sessionId,
                artifactType,
                className,
                content,
                1,
                null,
                "req-ai",
                LlmProvider.CLAUDE_CODE,
                TaskType.SPRING_BOOT_GENERATION,
                Instant.now());
    }
}
