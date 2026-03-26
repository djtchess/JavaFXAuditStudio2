package ff.ss.javaFxAuditStudio.domain.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de l'enum LlmProvider (IAP-2).
 */
class LlmProviderTest {

    @ParameterizedTest
    @CsvSource({
        "claude-code, CLAUDE_CODE",
        "CLAUDE-CODE, CLAUDE_CODE",
        "openai-gpt54, OPENAI_GPT54",
        "OPENAI-GPT54, OPENAI_GPT54",
        "claude-code-cli, CLAUDE_CODE_CLI",
        "none, NONE"
    })
    void fromString_should_resolve_provider(final String input, final String expected) {
        assertThat(LlmProvider.fromString(input)).isEqualTo(LlmProvider.valueOf(expected));
    }

    @Test
    void fromString_should_return_none_for_null() {
        assertThat(LlmProvider.fromString(null)).isEqualTo(LlmProvider.NONE);
    }

    @Test
    void fromString_should_return_none_for_blank() {
        assertThat(LlmProvider.fromString("   ")).isEqualTo(LlmProvider.NONE);
    }

    @Test
    void fromString_should_return_none_for_unknown_value() {
        assertThat(LlmProvider.fromString("unknown-provider")).isEqualTo(LlmProvider.NONE);
    }

    @Test
    void value_should_return_json_string() {
        assertThat(LlmProvider.CLAUDE_CODE.value()).isEqualTo("claude-code");
        assertThat(LlmProvider.OPENAI_GPT54.value()).isEqualTo("openai-gpt54");
        assertThat(LlmProvider.CLAUDE_CODE_CLI.value()).isEqualTo("claude-code-cli");
        assertThat(LlmProvider.NONE.value()).isEqualTo("none");
    }
}
