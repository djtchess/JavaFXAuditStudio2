package ff.ss.javaFxAuditStudio.adapters.out.ai;

import java.time.Duration;
import java.util.Map;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingAiEnrichmentAdapterTest {

    @Mock
    private ClaudeCodeAiEnrichmentAdapter claudeAdapter;

    @Mock
    private OpenAiGpt54AiEnrichmentAdapter openAiAdapter;

    @Mock
    private ClaudeCodeCliAiEnrichmentAdapter cliAdapter;

    @Test
    void should_truncate_degradation_reason_with_configured_limit() {
        AiEnrichmentProperties properties = new AiEnrichmentProperties(
                true,
                "claude-code",
                10_000L,
                new AiEnrichmentProperties.Credentials("sk-test-key"),
                null,
                false,
                null,
                null,
                null,
                null,
                12);
        RoutingAiEnrichmentAdapter adapter = new RoutingAiEnrichmentAdapter(
                properties,
                claudeAdapter,
                openAiAdapter,
                cliAdapter,
                new AiCircuitBreaker(5, 50, Duration.ofSeconds(1)),
                new SimpleMeterRegistry());
        AiEnrichmentRequest request = new AiEnrichmentRequest(
                "req-1",
                new SanitizedBundle("bundle-1", "MyController", "class A {}", 16, "1.0", null),
                TaskType.NAMING,
                "enrichment-naming",
                Map.of());
        String longReason = "abcdefghijklmnopqrstuvwxyz";

        when(claudeAdapter.call(request)).thenThrow(new IllegalStateException(longReason));

        AiEnrichmentResult result = adapter.enrich(request);

        assertThat(result.degraded()).isTrue();
        assertThat(result.provider()).isEqualTo(LlmProvider.CLAUDE_CODE);
        assertThat(result.degradationReason()).isEqualTo("abcdefghijkl...");
    }

    @Test
    void should_record_request_metrics_and_tokens_when_call_succeeds() {
        AiEnrichmentProperties properties = new AiEnrichmentProperties(
                true,
                "claude-code",
                10_000L,
                new AiEnrichmentProperties.Credentials("sk-test-key"),
                null,
                false,
                null,
                null,
                null,
                null,
                12);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RoutingAiEnrichmentAdapter adapter = new RoutingAiEnrichmentAdapter(
                properties,
                claudeAdapter,
                openAiAdapter,
                cliAdapter,
                new AiCircuitBreaker(5, 50, Duration.ofSeconds(1)),
                meterRegistry);
        AiEnrichmentRequest request = new AiEnrichmentRequest(
                "req-2",
                new SanitizedBundle("bundle-2", "MyController", "class B {}", 16, "1.0", null),
                TaskType.NAMING,
                "enrichment-naming",
                Map.of());
        AiEnrichmentResult response = new AiEnrichmentResult(
                "req-2",
                false,
                "",
                Map.of("handler", "suggestion"),
                128,
                LlmProvider.CLAUDE_CODE);

        when(claudeAdapter.call(request)).thenReturn(response);

        AiEnrichmentResult result = adapter.enrich(request);

        assertThat(result.degraded()).isFalse();
        assertThat(meterRegistry.find("llm.requests.total")
                .tag("provider", "claude-code")
                .tag("taskType", "naming")
                .tag("status", "success")
                .counter()
                .count()).isEqualTo(1.0d);
        assertThat(meterRegistry.find("llm.requests.duration")
                .tag("provider", "claude-code")
                .tag("taskType", "naming")
                .tag("status", "success")
                .timer()
                .count()).isEqualTo(1L);
        assertThat(meterRegistry.find("llm.tokens.used")
                .tag("provider", "claude-code")
                .tag("taskType", "naming")
                .summary()
                .totalAmount()).isEqualTo(128.0d);
    }
}
