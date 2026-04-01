package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.application.ports.in.ListLlmAuditEntriesUseCase;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
            "app.security.api-key-enabled=true",
            "app.security.api-key=sec-test-key"
        })
@ActiveProfiles("test")
class ApiSecurityConfigurationIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @MockitoBean
    private ListLlmAuditEntriesUseCase listLlmAuditEntriesUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters((Filter) springSecurityFilterChain)
                .build();
    }

    @Test
    void publicWorkbenchEndpoint_remainsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/workbench/overview"))
                .andExpect(status().isOk());
    }

    @Test
    void llmAuditEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/analysis/sessions/security-it/llm-audit"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Authentification requise"));
    }

    @Test
    void llmAuditEndpoint_allowsBearerAuthentication() throws Exception {
        when(listLlmAuditEntriesUseCase.handle("security-it")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/analysis/sessions/security-it/llm-audit")
                        .header("Authorization", "Bearer sec-test-key"))
                .andExpect(status().isOk());
    }

    @Test
    void projectAnalysisEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/projects/analysis/dependencies")
                        .contentType("application/json")
                        .content("""
                                {"projectId":"project-sec","controllerRefs":["A.java"]}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aiStatusEndpoint_allowsQueryParameterFallback() throws Exception {
        mockMvc.perform(get("/api/v1/ai-enrichment/status")
                        .queryParam("apiKey", "sec-test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("claude-code"));
    }

    @Test
    void prometheusEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void prometheusEndpoint_allowsBearerAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer sec-test-key"))
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpoint_remainsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void corsPreflight_remainsAllowedOnProtectedEndpoint() throws Exception {
        mockMvc.perform(options("/api/v1/analysis/sessions/security-it/llm-audit")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, POST.name()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }
}
