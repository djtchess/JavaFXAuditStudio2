package ff.ss.javaFxAuditStudio.adapters.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ActuatorObservabilityIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void healthEndpoint_exposesCustomIndicators() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.analysisWorkflow.status").value("UP"))
                .andExpect(jsonPath("$.components.llmEnrichment.status").value("UP"));
    }

    @Test
    void metricsEndpoint_exposesSessionGauge() throws Exception {
        mockMvc.perform(get("/actuator/metrics/jas.analysis.sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("jas.analysis.sessions"))
                .andExpect(jsonPath("$.availableTags[?(@.tag=='status')]").exists());
    }
}
