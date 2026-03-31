package ff.ss.javaFxAuditStudio.adapters.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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

    @Autowired
    private MeterRegistry meterRegistry;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        resetMeter("llm.requests.total");
        resetMeter("llm.requests.duration");
        resetMeter("llm.tokens.used");
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

    @Test
    void infoEndpoint_exposesBuildMetadata() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.build.version").exists())
                .andExpect(jsonPath("$.build.time").exists())
                .andExpect(jsonPath("$.git.commit.id").exists());
    }

    @Test
    void prometheusEndpoint_exposesConfiguredMetrics() throws Exception {
        Counter.builder("llm.requests.total")
                .tags("provider", "claude-code", "taskType", "naming", "status", "success")
                .register(meterRegistry)
                .increment();
        Timer.builder("llm.requests.duration")
                .tags("provider", "all", "taskType", "all", "status", "all")
                .register(meterRegistry)
                .record(Duration.ofMillis(120));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("llm_requests_total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jas_analysis_sessions")));
    }

    @Test
    void aiHealthEndpoint_exposesAggregatedLlmSnapshot() throws Exception {
        Counter.builder("llm.requests.total")
                .tags("provider", "claude-code", "taskType", "naming", "status", "success")
                .register(meterRegistry)
                .increment(3.0d);
        Counter.builder("llm.requests.total")
                .tags("provider", "claude-code", "taskType", "naming", "status", "failure")
                .register(meterRegistry)
                .increment();
        Counter.builder("llm.requests.total")
                .tags("provider", "all", "taskType", "all", "status", "all")
                .register(meterRegistry)
                .increment(4.0d);
        Timer.builder("llm.requests.duration")
                .tags("provider", "all", "taskType", "all", "status", "all")
                .register(meterRegistry)
                .record(Duration.ofMillis(250));
        DistributionSummary.builder("llm.tokens.used")
                .baseUnit("tokens")
                .tags("provider", "all", "taskType", "all")
                .register(meterRegistry)
                .record(640);

        mockMvc.perform(get("/actuator/ai-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("claude-code"))
                .andExpect(jsonPath("$.circuitBreakerState").value("CLOSED"))
                .andExpect(jsonPath("$.totalRequests").value(4))
                .andExpect(jsonPath("$.outcomes.success").value(3))
                .andExpect(jsonPath("$.outcomes.failure").value(1))
                .andExpect(jsonPath("$.totalTokens").value(640.0d));
    }

    private void resetMeter(final String meterName) {
        meterRegistry.find(meterName).meters().forEach(meterRegistry::remove);
    }
}
