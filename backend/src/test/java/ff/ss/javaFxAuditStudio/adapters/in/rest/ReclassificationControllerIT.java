package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.application.ports.in.ReclassifyRuleUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ReclassificationAuditEntry;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration MockMvc pour ReclassificationController (JAS-013).
 * Utilise @SpringBootTest(MOCK) + profil "test" (H2, flyway desactive).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ReclassificationControllerIT {

    @Autowired
    private WebApplicationContext wac;

    @MockitoBean
    private ReclassifyRuleUseCase reclassifyRuleUseCase;

    @MockitoBean
    private ReclassificationAuditPort reclassificationAuditPort;

    private MockMvc mockMvc;

    private static final String ANALYSIS_ID = "session-test-123";
    private static final String RULE_ID = "RG-001";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void patch_returns200_withUpdatedRule_whenReclassificationSucceeds() throws Exception {
        BusinessRule updatedRule = new BusinessRule(
                RULE_ID,
                "Description de test",
                "com/example/MyController.java",
                42,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false);

        when(reclassifyRuleUseCase.reclassify(
                eq(ANALYSIS_ID), eq(RULE_ID), eq(ResponsibilityClass.APPLICATION), any()))
                .thenReturn(updatedRule);

        mockMvc.perform(patch("/api/v1/analyses/{analysisId}/rules/{ruleId}/classification",
                        ANALYSIS_ID, RULE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"APPLICATION\", \"reason\": \"logique metier\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId").value(RULE_ID))
                .andExpect(jsonPath("$.responsibilityClass").value("APPLICATION"))
                .andExpect(jsonPath("$.extractionCandidate").value("USE_CASE"));
    }

    @Test
    void patch_returns409_whenSessionIsLocked() throws Exception {
        when(reclassifyRuleUseCase.reclassify(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Session verrouilee (statut LOCKED)"));

        mockMvc.perform(patch("/api/v1/analyses/{analysisId}/rules/{ruleId}/classification",
                        ANALYSIS_ID, RULE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"APPLICATION\", \"reason\": \"test\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void patch_returns404_whenRuleNotFound() throws Exception {
        when(reclassifyRuleUseCase.reclassify(any(), any(), any(), any()))
                .thenThrow(new NoSuchElementException("Regle introuvable"));

        mockMvc.perform(patch("/api/v1/analyses/{analysisId}/rules/{ruleId}/classification",
                        ANALYSIS_ID, RULE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"APPLICATION\", \"reason\": \"test\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_returns400_whenCategoryIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/v1/analyses/{analysisId}/rules/{ruleId}/classification",
                        ANALYSIS_ID, RULE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"CATEGORIE_INVALIDE\", \"reason\": \"test\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistory_returns200_withAuditEntries() throws Exception {
        ReclassificationAuditEntry entry = new ReclassificationAuditEntry(
                "audit-uuid-1",
                ANALYSIS_ID,
                RULE_ID,
                ResponsibilityClass.UI,
                ResponsibilityClass.APPLICATION,
                "raison de test",
                Instant.parse("2026-01-15T10:00:00Z"));

        when(reclassificationAuditPort.findByAnalysisIdAndRuleId(ANALYSIS_ID, RULE_ID))
                .thenReturn(List.of(entry));

        mockMvc.perform(get("/api/v1/analyses/{analysisId}/rules/{ruleId}/classification/history",
                        ANALYSIS_ID, RULE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ruleId").value(RULE_ID))
                .andExpect(jsonPath("$[0].fromCategory").value("UI"))
                .andExpect(jsonPath("$[0].toCategory").value("APPLICATION"))
                .andExpect(jsonPath("$[0].reason").value("raison de test"));
    }

    @Test
    void getHistory_returns200_withEmptyList_whenNoHistory() throws Exception {
        when(reclassificationAuditPort.findByAnalysisIdAndRuleId(ANALYSIS_ID, RULE_ID))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/analyses/{analysisId}/rules/{ruleId}/classification/history",
                        ANALYSIS_ID, RULE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
