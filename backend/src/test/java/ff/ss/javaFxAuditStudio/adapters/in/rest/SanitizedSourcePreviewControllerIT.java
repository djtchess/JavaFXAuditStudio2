package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.application.ports.in.PreviewSanitizedSourceUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedSourcePreviewResult;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationReport;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationTransformation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration MockMvc pour SanitizedSourcePreviewController (QW-4).
 *
 * <p>Verifie que la reponse de l'endpoint preview-sanitized expose les transformations
 * du rapport de sanitisation et le statut approved/refused (CA-2, CA-3).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class SanitizedSourcePreviewControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private PreviewSanitizedSourceUseCase previewSanitizedSourceUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void should_return_transformation_counts_and_approved_status_when_report_present() throws Exception {
        List<SanitizationTransformation> transformations = List.of(
                new SanitizationTransformation(SanitizationRuleType.IDENTIFIER_REPLACEMENT, 3, "3 identifiers replaced"),
                new SanitizationTransformation(SanitizationRuleType.SECRET_REMOVAL, 1, "1 secret removed"));

        SanitizationReport report = SanitizationReport.approved("bundle-test", "1.0", transformations);
        SanitizedBundle bundle = new SanitizedBundle(
                "bundle-test",
                "MyController",
                "sanitized source",
                10,
                "1.0",
                report);
        SanitizedSourcePreviewResult result = new SanitizedSourcePreviewResult(bundle, true);

        when(previewSanitizedSourceUseCase.preview(eq("sess-preview-1"))).thenReturn(result);

        mockMvc.perform(post("/api/v1/analysis/sessions/sess-preview-1/preview-sanitized"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sanitized").value(true))
                .andExpect(jsonPath("$.reportApproved").value(true))
                .andExpect(jsonPath("$.transformationCountsByRuleType.IDENTIFIER_REPLACEMENT", is(3)))
                .andExpect(jsonPath("$.transformationCountsByRuleType.SECRET_REMOVAL", is(1)));
    }

    @Test
    void should_return_null_report_fields_when_bundle_has_no_report() throws Exception {
        SanitizedBundle bundle = new SanitizedBundle(
                "bundle-no-report",
                "MyController",
                "raw source",
                10,
                "1.0",
                null);
        SanitizedSourcePreviewResult result = new SanitizedSourcePreviewResult(bundle, false);

        when(previewSanitizedSourceUseCase.preview(eq("sess-preview-2"))).thenReturn(result);

        mockMvc.perform(post("/api/v1/analysis/sessions/sess-preview-2/preview-sanitized"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sanitized").value(false))
                .andExpect(jsonPath("$.reportApproved").doesNotExist())
                .andExpect(jsonPath("$.transformationCountsByRuleType").doesNotExist());
    }

    @Test
    void should_return_404_when_session_not_found() throws Exception {
        when(previewSanitizedSourceUseCase.preview(eq("unknown")))
                .thenThrow(new IllegalArgumentException("Session introuvable : unknown"));

        mockMvc.perform(post("/api/v1/analysis/sessions/unknown/preview-sanitized"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_refused_status_when_report_is_refused() throws Exception {
        List<SanitizationTransformation> transformations = List.of(
                new SanitizationTransformation(SanitizationRuleType.SECRET_REMOVAL, 0, "no secrets removed"));

        SanitizationReport report = SanitizationReport.refused("bundle-refused", "1.0", transformations);
        SanitizedBundle bundle = new SanitizedBundle(
                "bundle-refused",
                "SensitiveController",
                "raw source with markers",
                15,
                "1.0",
                report);
        SanitizedSourcePreviewResult result = new SanitizedSourcePreviewResult(bundle, false);

        when(previewSanitizedSourceUseCase.preview(eq("sess-preview-3"))).thenReturn(result);

        mockMvc.perform(post("/api/v1/analysis/sessions/sess-preview-3/preview-sanitized"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sanitized").value(false))
                .andExpect(jsonPath("$.reportApproved").value(false))
                .andExpect(jsonPath("$.transformationCountsByRuleType.SECRET_REMOVAL", is(0)));
    }
}
