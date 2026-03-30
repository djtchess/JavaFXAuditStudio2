package ff.ss.javaFxAuditStudio.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ff.ss.javaFxAuditStudio.application.ports.out.AiArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiArtifactZipExport;
import ff.ss.javaFxAuditStudio.domain.ai.AiGeneratedArtifact;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;

@ExtendWith(MockitoExtension.class)
class AiArtifactCatalogServiceTest {

    @Mock
    private AnalysisSessionPort sessionPort;

    @Mock
    private AiArtifactPersistencePort aiArtifactPersistencePort;

    @Test
    void should_export_latest_ai_artifacts_using_java_package_paths() throws Exception {
        AnalysisSession session = new AnalysisSession(
                "sess-zip",
                "Controller",
                "Controller.java",
                AnalysisStatus.COMPLETED,
                Instant.now());
        when(sessionPort.findById("sess-zip")).thenReturn(Optional.of(session));
        when(aiArtifactPersistencePort.findLatestBySessionId("sess-zip")).thenReturn(List.of(
                artifact(
                        "USE_CASE",
                        "PatientUseCase",
                        "package ff.example.usecase;\npublic interface PatientUseCase {}"),
                artifact(
                        "VIEW_MODEL",
                        "PatientViewModel",
                        "public class PatientViewModel {}")));

        AiArtifactCatalogService service = new AiArtifactCatalogService(sessionPort, aiArtifactPersistencePort);

        AiArtifactZipExport export = service.export("sess-zip");

        assertThat(export.artifactCount()).isEqualTo(2);
        assertThat(listZipEntries(export.content()))
                .contains("ff/example/usecase/PatientUseCase.java", "PatientViewModel.java");
    }

    @Test
    void should_ignore_incomplete_ai_artifacts_during_zip_export() throws Exception {
        AnalysisSession session = new AnalysisSession(
                "sess-zip",
                "Controller",
                "Controller.java",
                AnalysisStatus.COMPLETED,
                Instant.now());
        when(sessionPort.findById("sess-zip")).thenReturn(Optional.of(session));
        when(aiArtifactPersistencePort.findLatestBySessionId("sess-zip")).thenReturn(List.of(
                artifact(
                        "USE_CASE",
                        "PatientUseCase",
                        "package ff.example.usecase;\npublic class PatientUseCase {}"),
                artifact(
                        "POLICY",
                        "PatientPolicy",
                        "package ff.example.policy;\npublic class PatientPolicy {\n    // TODO: implementer\n}")));

        AiArtifactCatalogService service = new AiArtifactCatalogService(sessionPort, aiArtifactPersistencePort);

        AiArtifactZipExport export = service.export("sess-zip");

        assertThat(export.artifactCount()).isEqualTo(1);
        assertThat(listZipEntries(export.content()))
                .containsExactly("ff/example/usecase/PatientUseCase.java");
    }

    @Test
    void should_fail_zip_export_when_only_incomplete_ai_artifacts_exist() {
        AnalysisSession session = new AnalysisSession(
                "sess-zip",
                "Controller",
                "Controller.java",
                AnalysisStatus.COMPLETED,
                Instant.now());
        when(sessionPort.findById("sess-zip")).thenReturn(Optional.of(session));
        when(aiArtifactPersistencePort.findLatestBySessionId("sess-zip")).thenReturn(List.of(
                artifact(
                        "POLICY",
                        "PatientPolicy",
                        "package ff.example.policy;\npublic class PatientPolicy {\n    // TODO: implementer\n}")));

        AiArtifactCatalogService service = new AiArtifactCatalogService(sessionPort, aiArtifactPersistencePort);

        assertThatThrownBy(() -> service.export("sess-zip"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aucun artefact IA exportable");
    }

    private static AiGeneratedArtifact artifact(
            final String artifactType,
            final String className,
            final String content) {
        return new AiGeneratedArtifact(
                artifactType + "-v1",
                "sess-zip",
                artifactType,
                className,
                content,
                1,
                null,
                "req-1",
                LlmProvider.CLAUDE_CODE,
                TaskType.SPRING_BOOT_GENERATION,
                Instant.now());
    }

    private static List<String> listZipEntries(final byte[] content) throws Exception {
        List<String> entryNames = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(
                new ByteArrayInputStream(content),
                StandardCharsets.UTF_8)) {
            java.util.zip.ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                entryNames.add(entry.getName());
                entry = zipInputStream.getNextEntry();
            }
        }
        return entryNames;
    }
}
