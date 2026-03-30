package ff.ss.javaFxAuditStudio.adapters.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ff.ss.javaFxAuditStudio.application.ports.in.ExportAiGeneratedArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ListAiGeneratedArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.RefineAiArtifactUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.VerifyAiArtifactCoherenceUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.AiArtifactZipExport;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;
import ff.ss.javaFxAuditStudio.domain.ai.AiGeneratedArtifact;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AiGeneratedArtifactControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private ListAiGeneratedArtifactsUseCase listAiGeneratedArtifactsUseCase;

    @MockitoBean
    private RefineAiArtifactUseCase refineAiArtifactUseCase;

    @MockitoBean
    private VerifyAiArtifactCoherenceUseCase verifyAiArtifactCoherenceUseCase;

    @MockitoBean
    private ExportAiGeneratedArtifactsUseCase exportAiGeneratedArtifactsUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void should_return_versions_for_supported_artifact_type() throws Exception {
        when(listAiGeneratedArtifactsUseCase.listVersions("sess-1", "USE_CASE"))
                .thenReturn(List.of(aiArtifact("USE_CASE", "PatientUseCase", 2)));

        mockMvc.perform(get("/api/v1/analyses/sess-1/artifacts/ai/USE_CASE/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("sess-1"))
                .andExpect(jsonPath("$.artifacts[0].artifactType").value("USE_CASE"))
                .andExpect(jsonPath("$.artifacts[0].versionNumber").value(2))
                .andExpect(jsonPath("$.artifacts[0].implementationStatus").value("READY"));
    }

    @Test
    void should_expose_incomplete_status_for_artifact_with_todo_placeholder() throws Exception {
        when(listAiGeneratedArtifactsUseCase.listLatest("sess-1"))
                .thenReturn(List.of(aiArtifact(
                        "POLICY",
                        "PatientPolicy",
                        3,
                        "package ff.example.policy;\npublic class PatientPolicy {\n    // TODO: implementer\n    boolean isReady() {\n        return false;\n    }\n}")));

        mockMvc.perform(get("/api/v1/analyses/sess-1/artifacts/ai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifacts[0].artifactType").value("POLICY"))
                .andExpect(jsonPath("$.artifacts[0].implementationStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.artifacts[0].implementationWarning")
                        .value("Artefact IA incomplet detecte : placeholder d'implementation residuel."));
    }

    @Test
    void should_return_400_for_invalid_artifact_type_on_versions() throws Exception {
        mockMvc.perform(get("/api/v1/analyses/sess-1/artifacts/ai/not-valid/versions"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_for_invalid_artifact_type_on_refine_alias() throws Exception {
        mockMvc.perform(post("/api/v1/analyses/sess-1/generate/ai/refine")
                        .contentType("application/json")
                        .content("""
                                {"artifactType":"not-valid","instruction":"ameliore","previousCode":"code"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_404_when_refine_alias_session_is_unknown() throws Exception {
        when(refineAiArtifactUseCase.refine(eq("missing"), any()))
                .thenThrow(new IllegalArgumentException("Session introuvable"));

        mockMvc.perform(post("/api/v1/analyses/missing/generate/ai/refine")
                        .contentType("application/json")
                        .content("""
                                {"artifactType":"USE_CASE","instruction":"ameliore","previousCode":"code"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_zip_payload_for_export_alias() throws Exception {
        when(exportAiGeneratedArtifactsUseCase.export("sess-1"))
                .thenReturn(new AiArtifactZipExport("ai-artifacts-sess-1.zip", new byte[] {1, 2, 3}, 1));

        mockMvc.perform(get("/api/v1/analyses/sess-1/generate/ai/export/zip"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"))
                .andExpect(header().string("X-AI-Artifact-Count", "1"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"ai-artifacts-sess-1.zip\""));
    }

    private static AiGeneratedArtifact aiArtifact(
            final String artifactType,
            final String className,
            final int versionNumber) {
        return aiArtifact(
                artifactType,
                className,
                versionNumber,
                "package ff.example;\npublic class " + className + " {}");
    }

    private static AiGeneratedArtifact aiArtifact(
            final String artifactType,
            final String className,
            final int versionNumber,
            final String content) {
        return new AiGeneratedArtifact(
                "version-" + versionNumber,
                "sess-1",
                artifactType,
                className,
                content,
                versionNumber,
                null,
                "req-" + versionNumber,
                LlmProvider.CLAUDE_CODE,
                TaskType.SPRING_BOOT_GENERATION,
                Instant.parse("2026-03-27T07:00:00Z"));
    }
}
