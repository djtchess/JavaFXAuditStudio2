package ff.ss.javaFxAuditStudio.configuration;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ff.ss.javaFxAuditStudio.domain.ai.TaskType;

import static org.assertj.core.api.Assertions.assertThat;

class AiEnrichmentPropertiesTest {

    private static AiEnrichmentProperties propertiesWith(final Map<TaskType, Integer> maxTokensByTask) {
        return new AiEnrichmentProperties(
                true,
                "claude-code",
                10_000L,
                new AiEnrichmentProperties.Credentials("sk-test-key"),
                null,
                true,
                null,
                null,
                maxTokensByTask,
                null,
                null,
                null);
    }

    @Test
    void should_return_default_max_tokens_when_configuration_is_absent() {
        AiEnrichmentProperties properties = propertiesWith(null);

        assertThat(properties.effectiveMaxTokens(TaskType.NAMING)).isEqualTo(1024);
        assertThat(properties.effectiveMaxTokens(TaskType.DESCRIPTION)).isEqualTo(1024);
        assertThat(properties.effectiveMaxTokens(TaskType.CLASSIFICATION_HINT)).isEqualTo(1024);
        assertThat(properties.effectiveMaxTokens(TaskType.ARTIFACT_REVIEW)).isEqualTo(2048);
        assertThat(properties.effectiveMaxTokens(TaskType.SPRING_BOOT_GENERATION)).isEqualTo(4096);
    }

    @Test
    void should_return_configured_max_tokens_when_task_specific_value_exists() {
        AiEnrichmentProperties properties = propertiesWith(Map.of(
                TaskType.SPRING_BOOT_GENERATION, 8192,
                TaskType.ARTIFACT_REVIEW, 3072));

        assertThat(properties.effectiveMaxTokens(TaskType.SPRING_BOOT_GENERATION)).isEqualTo(8192);
        assertThat(properties.effectiveMaxTokens(TaskType.ARTIFACT_REVIEW)).isEqualTo(3072);
    }

    @Test
    void should_fallback_to_default_when_configured_value_is_invalid() {
        AiEnrichmentProperties properties = propertiesWith(Map.of(
                TaskType.NAMING, 0,
                TaskType.ARTIFACT_REVIEW, -1));

        assertThat(properties.effectiveMaxTokens(TaskType.NAMING)).isEqualTo(1024);
        assertThat(properties.effectiveMaxTokens(TaskType.ARTIFACT_REVIEW)).isEqualTo(2048);
    }

    @Test
    void should_return_standard_default_when_task_type_is_null() {
        AiEnrichmentProperties properties = propertiesWith(null);

        assertThat(properties.effectiveMaxTokens(null)).isEqualTo(1024);
    }

    @Test
    void should_recognize_openai_codex_cli_as_supported_provider_without_credential() {
        AiEnrichmentProperties properties = new AiEnrichmentProperties(
                true,
                "openai-codex-cli",
                10_000L,
                null,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(properties.isSupportedProvider()).isTrue();
        assertThat(properties.isCredentialRequired()).isFalse();
        assertThat(properties.providerEnum().value()).isEqualTo("openai-codex-cli");
    }

    @Test
    void should_return_provider_specific_cli_defaults_for_openai_codex_cli() {
        AiEnrichmentProperties properties = new AiEnrichmentProperties(
                true,
                "openai-codex-cli",
                10_000L,
                null,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(properties.effectiveCliCommand()).isEqualTo("codex");
        assertThat(properties.effectiveCliModel()).isEqualTo("gpt-5.3-codex");
    }

    @Test
    void should_preserve_explicit_cli_model_when_configured() {
        AiEnrichmentProperties properties = new AiEnrichmentProperties(
                true,
                "openai-codex-cli",
                10_000L,
                null,
                null,
                true,
                "codex",
                "gpt-5.4",
                null,
                null,
                null,
                null,
                null);

        assertThat(properties.effectiveCliCommand()).isEqualTo("codex");
        assertThat(properties.effectiveCliModel()).isEqualTo("gpt-5.4");
    }

    @Test
    void should_return_default_max_response_size_when_configuration_is_absent() {
        AiEnrichmentProperties properties = propertiesWith(null);

        assertThat(properties.effectiveMaxResponseSizeBytes()).isEqualTo(512 * 1024);
    }

    @Test
    void should_return_configured_max_response_size_when_present() {
        AiEnrichmentProperties properties = new AiEnrichmentProperties(
                true,
                "claude-code",
                10_000L,
                new AiEnrichmentProperties.Credentials("sk-test-key"),
                null,
                true,
                null,
                null,
                null,
                2048,
                null,
                null);

        assertThat(properties.effectiveMaxResponseSizeBytes()).isEqualTo(2048);
    }

    @Test
    void should_fallback_to_default_max_response_size_when_value_is_invalid() {
        AiEnrichmentProperties properties = new AiEnrichmentProperties(
                true,
                "claude-code",
                10_000L,
                new AiEnrichmentProperties.Credentials("sk-test-key"),
                null,
                true,
                null,
                null,
                null,
                0,
                null,
                null);

        assertThat(properties.effectiveMaxResponseSizeBytes()).isEqualTo(512 * 1024);
    }

    @Test
    void should_return_default_max_degradation_reason_length_when_configuration_is_absent() {
        AiEnrichmentProperties properties = propertiesWith(null);

        assertThat(properties.effectiveMaxDegradationReasonLength()).isEqualTo(200);
    }

    @Test
    void should_return_configured_max_degradation_reason_length_when_present() {
        AiEnrichmentProperties properties = new AiEnrichmentProperties(
                true,
                "claude-code",
                10_000L,
                new AiEnrichmentProperties.Credentials("sk-test-key"),
                null,
                true,
                null,
                null,
                null,
                null,
                64,
                null);

        assertThat(properties.effectiveMaxDegradationReasonLength()).isEqualTo(64);
    }

    @Test
    void should_fallback_to_default_max_degradation_reason_length_when_value_is_invalid() {
        AiEnrichmentProperties properties = new AiEnrichmentProperties(
                true,
                "claude-code",
                10_000L,
                new AiEnrichmentProperties.Credentials("sk-test-key"),
                null,
                true,
                null,
                null,
                null,
                null,
                0,
                null);

        assertThat(properties.effectiveMaxDegradationReasonLength()).isEqualTo(200);
    }

    @Test
    void should_return_default_prompt_context_budget_when_configuration_is_absent() {
        AiEnrichmentProperties properties = propertiesWith(null);

        AiEnrichmentProperties.PromptContextBudget budget =
                properties.effectivePromptContextBudget(TaskType.ARTIFACT_COHERENCE);

        assertThat(budget.maxCodeFragmentChars()).isEqualTo(8_000);
        assertThat(budget.maxInstructionChars()).isEqualTo(1_500);
        assertThat(budget.maxArtifactDetailsChars()).isEqualTo(12_000);
        assertThat(budget.maxArtifactDetailsItems()).isEqualTo(5);
        assertThat(budget.maxReferencePatternsChars()).isEqualTo(8_000);
        assertThat(budget.maxReferencePatternsItems()).isEqualTo(5);
    }

    @Test
    void should_return_configured_prompt_context_budget_when_present() {
        AiEnrichmentProperties properties = new AiEnrichmentProperties(
                true,
                "claude-code",
                10_000L,
                new AiEnrichmentProperties.Credentials("sk-test-key"),
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                Map.of(TaskType.ARTIFACT_REFINEMENT, new AiEnrichmentProperties.PromptContextBudget(
                        256,
                        128,
                        512,
                        2,
                        640,
                        3)));

        AiEnrichmentProperties.PromptContextBudget budget =
                properties.effectivePromptContextBudget(TaskType.ARTIFACT_REFINEMENT);

        assertThat(budget.maxCodeFragmentChars()).isEqualTo(256);
        assertThat(budget.maxInstructionChars()).isEqualTo(128);
        assertThat(budget.maxArtifactDetailsChars()).isEqualTo(512);
        assertThat(budget.maxArtifactDetailsItems()).isEqualTo(2);
        assertThat(budget.maxReferencePatternsChars()).isEqualTo(640);
        assertThat(budget.maxReferencePatternsItems()).isEqualTo(3);
    }

    @Test
    void should_fallback_to_default_prompt_context_budget_when_configured_value_is_invalid() {
        AiEnrichmentProperties properties = new AiEnrichmentProperties(
                true,
                "claude-code",
                10_000L,
                new AiEnrichmentProperties.Credentials("sk-test-key"),
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                Map.of(TaskType.ARTIFACT_REVIEW, new AiEnrichmentProperties.PromptContextBudget(
                        0,
                        -1,
                        null,
                        0,
                        -5,
                        null)));

        AiEnrichmentProperties.PromptContextBudget budget =
                properties.effectivePromptContextBudget(TaskType.ARTIFACT_REVIEW);

        assertThat(budget.maxCodeFragmentChars()).isEqualTo(6_000);
        assertThat(budget.maxInstructionChars()).isEqualTo(1_500);
        assertThat(budget.maxArtifactDetailsChars()).isEqualTo(8_000);
        assertThat(budget.maxArtifactDetailsItems()).isEqualTo(4);
        assertThat(budget.maxReferencePatternsChars()).isEqualTo(6_000);
        assertThat(budget.maxReferencePatternsItems()).isEqualTo(4);
    }
}
