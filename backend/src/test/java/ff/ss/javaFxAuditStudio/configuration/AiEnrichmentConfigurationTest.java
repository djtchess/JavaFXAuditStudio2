package ff.ss.javaFxAuditStudio.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiEnrichmentConfigurationTest {

    private static AiEnrichmentConfiguration buildConfig(
            final boolean enabled,
            final String provider,
            final String claudeKey,
            final String openaiKey) {
        var claudeCreds = claudeKey != null ? new AiEnrichmentProperties.Credentials(claudeKey) : null;
        var openaiCreds = openaiKey != null ? new AiEnrichmentProperties.Credentials(openaiKey) : null;
        var props = new AiEnrichmentProperties(enabled, provider, 10_000L, claudeCreds, openaiCreds, true);
        return new AiEnrichmentConfiguration(props);
    }

    @Test
    void should_not_throw_when_disabled() {
        var config = buildConfig(false, null, null, null);
        assertThatCode(config::validateConfiguration).doesNotThrowAnyException();
    }

    @Test
    void should_not_throw_when_enabled_with_valid_claude_credentials() {
        var config = buildConfig(true, "claude-code", "sk-test-key", null);
        assertThatCode(config::validateConfiguration).doesNotThrowAnyException();
    }

    @Test
    void should_not_throw_when_enabled_with_valid_openai_credentials() {
        var config = buildConfig(true, "openai-gpt54", null, "sk-openai-key");
        assertThatCode(config::validateConfiguration).doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_enabled_with_unknown_provider() {
        var config = buildConfig(true, "gpt-99", "some-key", null);
        assertThatThrownBy(config::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("claude-code")
                .hasMessageContaining("openai-gpt54");
    }

    @Test
    void should_throw_when_enabled_with_blank_provider() {
        var config = buildConfig(true, "  ", "some-key", null);
        assertThatThrownBy(config::validateConfiguration)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_throw_when_enabled_with_null_provider() {
        var config = buildConfig(true, null, "some-key", null);
        assertThatThrownBy(config::validateConfiguration)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_throw_when_enabled_claude_provider_but_credential_blank() {
        var config = buildConfig(true, "claude-code", "  ", null);
        assertThatThrownBy(config::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("claude-code");
    }

    @Test
    void should_throw_when_enabled_openai_provider_but_credential_missing() {
        var config = buildConfig(true, "openai-gpt54", null, null);
        assertThatThrownBy(config::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("openai-gpt54");
    }
}
