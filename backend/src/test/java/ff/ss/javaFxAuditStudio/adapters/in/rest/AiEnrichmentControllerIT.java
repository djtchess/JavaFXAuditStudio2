package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.application.ports.in.EnrichAnalysisUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration MockMvc pour AiEnrichmentController (JAS-017).
 * Utilise @SpringBootTest(MOCK) + profil "test" (H2, flyway desactive).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AiEnrichmentControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private EnrichAnalysisUseCase enrichAnalysisUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void should_return_200_with_nominal_result() throws Exception {
        AiEnrichmentResult result = new AiEnrichmentResult(
                "req-abc", false, "", Map.of("onSave", "Save action"), 42, LlmProvider.CLAUDE_CODE);
        when(enrichAnalysisUseCase.enrich("sess-1", TaskType.NAMING)).thenReturn(result);

        mockMvc.perform(post("/api/v1/analysis/sessions/sess-1/enrich?taskType=NAMING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.degraded").value(false))
                .andExpect(jsonPath("$.provider").value("claude-code"))
                .andExpect(jsonPath("$.tokensUsed").value(42))
                .andExpect(jsonPath("$.requestId").value("req-abc"));
    }

    @Test
    void should_keep_legacy_plural_alias_operational() throws Exception {
        AiEnrichmentResult result = new AiEnrichmentResult(
                "req-legacy", false, "", Map.of("onSave", "Save action"), 21, LlmProvider.CLAUDE_CODE);
        when(enrichAnalysisUseCase.enrich("sess-legacy", TaskType.NAMING)).thenReturn(result);

        mockMvc.perform(post("/api/v1/analyses/sess-legacy/enrich?taskType=NAMING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-legacy"));
    }

    @Test
    void should_return_200_with_degraded_result_when_disabled() throws Exception {
        AiEnrichmentResult degraded = AiEnrichmentResult.degraded("req-deg", "AI enrichment disabled");
        when(enrichAnalysisUseCase.enrich("sess-2", TaskType.NAMING)).thenReturn(degraded);

        mockMvc.perform(post("/api/v1/analysis/sessions/sess-2/enrich"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.degraded").value(true))
                .andExpect(jsonPath("$.degradationReason").value("AI enrichment disabled"))
                .andExpect(jsonPath("$.provider").value("none"));
    }

    @Test
    void should_return_404_when_session_not_found() throws Exception {
        when(enrichAnalysisUseCase.enrich(any(), any()))
                .thenThrow(new IllegalArgumentException("Session introuvable : unknown"));

        mockMvc.perform(post("/api/v1/analysis/sessions/unknown/enrich"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_use_default_task_type_naming() throws Exception {
        AiEnrichmentResult result = AiEnrichmentResult.degraded("req-def", "disabled");
        when(enrichAnalysisUseCase.enrich("sess-3", TaskType.NAMING)).thenReturn(result);

        mockMvc.perform(post("/api/v1/analysis/sessions/sess-3/enrich"))
                .andExpect(status().isOk());
    }
}
