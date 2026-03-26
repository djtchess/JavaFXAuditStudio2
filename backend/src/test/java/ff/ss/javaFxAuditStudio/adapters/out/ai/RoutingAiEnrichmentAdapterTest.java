package ff.ss.javaFxAuditStudio.adapters.out.ai;

import java.time.Duration;
import java.util.Map;

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
                new AiCircuitBreaker(5, 50, Duration.ofSeconds(1)));
        AiEnrichmentRequest request = new AiEnrichmentRequest(
                "req-1",
                new SanitizedBundle("bundle-1", "MyController", "class A {}", 16, "1.0"),
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
}
