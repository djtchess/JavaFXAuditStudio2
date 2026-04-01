package ff.ss.javaFxAuditStudio.adapters.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import ff.ss.javaFxAuditStudio.application.ports.in.RefineArtifactUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ArtifactRefinementControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private RefineArtifactUseCase refineArtifactUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void should_return_200_for_nominal_refinement() throws Exception {
        when(refineArtifactUseCase.refine(any(), any())).thenReturn(
                new AiCodeGenerationResult(
                        "req-1",
                        false,
                        "",
                        Map.of("USE_CASE", "public interface RefinedUseCase {}"),
                        11,
                        LlmProvider.CLAUDE_CODE));

        mockMvc.perform(post("/api/v1/analysis/sessions/sess-1/refine")
                        .contentType("application/json")
                        .content("""
                                {"artifactType":"USE_CASE","instruction":"ameliore","previousCode":"code"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.degraded").value(false))
                .andExpect(jsonPath("$.generatedClasses.USE_CASE").exists());
    }

    @Test
    void should_return_404_when_session_not_found() throws Exception {
        when(refineArtifactUseCase.refine(any(), any()))
                .thenThrow(new IllegalArgumentException("Session introuvable : unknown"));

        mockMvc.perform(post("/api/v1/analysis/sessions/unknown/refine")
                        .contentType("application/json")
                        .content("""
                                {"artifactType":"USE_CASE","instruction":"ameliore","previousCode":"code"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_400_for_invalid_artifact_type() throws Exception {
        mockMvc.perform(post("/api/v1/analysis/sessions/sess-1/refine")
                        .contentType("application/json")
                        .content("""
                                {"artifactType":"NOT_A_TYPE","instruction":"ameliore","previousCode":"code"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
