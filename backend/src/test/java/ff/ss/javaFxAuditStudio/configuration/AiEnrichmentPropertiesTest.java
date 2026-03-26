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
                64);

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
                0);

        assertThat(properties.effectiveMaxDegradationReasonLength()).isEqualTo(200);
    }
}
