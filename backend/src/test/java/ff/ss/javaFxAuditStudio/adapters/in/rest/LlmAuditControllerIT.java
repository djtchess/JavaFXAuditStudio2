package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.application.ports.out.LlmAuditPort;
import ff.ss.javaFxAuditStudio.domain.ai.LlmAuditEntry;
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

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Tests d'integration MockMvc pour LlmAuditController (JAS-029).
 * Utilise @SpringBootTest(MOCK) + profil "test" (H2, flyway desactive).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class LlmAuditControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private LlmAuditPort llmAuditPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void should_return_empty_list_when_no_audits() throws Exception {
        when(llmAuditPort.findBySessionId("sess-empty")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/analysis/sessions/sess-empty/llm-audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void should_return_audit_entries_for_session() throws Exception {
        LlmAuditEntry entry = new LlmAuditEntry(
                "audit-uuid-1",
                "sess-with-data",
                Instant.parse("2026-03-23T10:00:00Z"),
                LlmProvider.CLAUDE_CODE,
                TaskType.NAMING,
                "1.0",
                "abc123def456abc123def456abc123def456abc123def456abc123def456abc1",
                100,
                false,
                "");
        when(llmAuditPort.findBySessionId("sess-with-data")).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/v1/analysis/sessions/sess-with-data/llm-audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].auditId").value("audit-uuid-1"))
                .andExpect(jsonPath("$[0].sessionId").value("sess-with-data"))
                .andExpect(jsonPath("$[0].provider").value("claude-code"))
                .andExpect(jsonPath("$[0].taskType").value("NAMING"))
                .andExpect(jsonPath("$[0].sanitizationVersion").value("1.0"))
                .andExpect(jsonPath("$[0].degraded").value(false))
                .andExpect(jsonPath("$[0].promptTokensEstimate").value(100));
    }

    @Test
    void should_not_expose_raw_content_in_response() throws Exception {
        String sensitiveContent = "public class MyRealController { private String password = \"secret\"; }";
        LlmAuditEntry entry = new LlmAuditEntry(
                "audit-uuid-2",
                "sess-privacy",
                Instant.now(),
                LlmProvider.OPENAI_GPT54,
                TaskType.DESCRIPTION,
                "1.0",
                "deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef",
                50,
                false,
                "");
        when(llmAuditPort.findBySessionId("sess-privacy")).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/v1/analysis/sessions/sess-privacy/llm-audit"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("sanitizedSource"))))
                .andExpect(content().string(not(containsString(sensitiveContent))))
                .andExpect(jsonPath("$[0].payloadHash").value(
                        "deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"));
    }
}
