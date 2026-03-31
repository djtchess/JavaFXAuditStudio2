package ff.ss.javaFxAuditStudio.adapters.out.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties;
import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties.Credentials;
import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties.Retry;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;

class LlmEnrichmentHealthIndicatorTest {

    @Test
    void health_isUp_whenEnrichmentIsDisabled() {
        LlmEnrichmentHealthIndicator indicator = new LlmEnrichmentHealthIndicator(properties(false, "claude-code", "secret"));

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(indicator.health().getDetails()).containsEntry("mode", "disabled");
        assertThat(indicator.health().getDetails()).containsEntry("enabled", false);
    }

    @Test
    void health_isDown_whenCredentialIsMissing() {
        LlmEnrichmentHealthIndicator indicator = new LlmEnrichmentHealthIndicator(properties(true, "claude-code", ""));

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(indicator.health().getDetails()).containsEntry("mode", "missing-credential");
    }

    @Test
    void health_isUp_whenOpenAiCodexCliDoesNotNeedCredential() {
        LlmEnrichmentHealthIndicator indicator =
                new LlmEnrichmentHealthIndicator(properties(true, "openai-codex-cli", ""));

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(indicator.health().getDetails()).containsEntry("credentialRequired", false);
        assertThat(indicator.health().getDetails()).containsEntry("credentialPresent", true);
    }

    private static AiEnrichmentProperties properties(
            final boolean enabled,
            final String provider,
            final String apiKey) {
        return new AiEnrichmentProperties(
                enabled,
                provider,
                30_000L,
                new Credentials(apiKey),
                new Credentials("openai-key"),
                true,
                "claude",
                new Retry(2, 500L, 2.0),
                Map.of(TaskType.NAMING, 1024),
                524_288,
                200,
                null);
    }
}
