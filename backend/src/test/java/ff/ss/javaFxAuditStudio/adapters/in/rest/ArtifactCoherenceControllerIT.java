package ff.ss.javaFxAuditStudio.adapters.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import ff.ss.javaFxAuditStudio.application.ports.in.VerifyArtifactCoherenceUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.ArtifactCoherenceResult;
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
class ArtifactCoherenceControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private VerifyArtifactCoherenceUseCase verifyArtifactCoherenceUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void should_return_200_for_nominal_coherence_verification() throws Exception {
        when(verifyArtifactCoherenceUseCase.verify(any())).thenReturn(
                new ArtifactCoherenceResult(
                        "req-1",
                        false,
                        "",
                        true,
                        Map.of("USE_CASE", "ok"),
                        List.of("tout est aligne"),
                        LlmProvider.OPENAI_GPT54));

        mockMvc.perform(post("/api/v1/analysis/sessions/sess-1/coherence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coherent").value(true))
                .andExpect(jsonPath("$.artifactIssues.USE_CASE").value("ok"));
    }

    @Test
    void should_return_404_when_session_not_found() throws Exception {
        when(verifyArtifactCoherenceUseCase.verify(any()))
                .thenThrow(new IllegalArgumentException("Session introuvable : unknown"));

        mockMvc.perform(post("/api/v1/analysis/sessions/unknown/coherence"))
                .andExpect(status().isNotFound());
    }
}
