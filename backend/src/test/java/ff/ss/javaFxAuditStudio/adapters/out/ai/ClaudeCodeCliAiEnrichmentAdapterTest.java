package ff.ss.javaFxAuditStudio.adapters.out.ai;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClaudeCodeCliAiEnrichmentAdapterTest {

    @TempDir
    Path tempDir;

    private PromptTemplateLoader templateLoader;
    private ObjectMapper objectMapper;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        templateLoader = new PromptTemplateLoader();
        objectMapper = new ObjectMapper();
        logger = (Logger) LoggerFactory.getLogger(ClaudeCodeCliAiEnrichmentAdapter.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
        appender.stop();
    }

    private static AiEnrichmentProperties properties() {
        return new AiEnrichmentProperties(
                true,
                "claude-code-cli",
                5_000L,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null);
    }

    private AiEnrichmentRequest requestFor(final String secretSource, final String promptTemplate) {
        SanitizedBundle bundle = new SanitizedBundle(
                "bundle-test",
                "MyController",
                secretSource,
                42,
                "1.0",
                null);
        return new AiEnrichmentRequest(
                UUID.randomUUID().toString(),
                bundle,
                TaskType.NAMING,
                promptTemplate,
                Map.of("extra", "value"));
    }

    private String createCliScript(final String fileName, final String body) throws IOException {
        Path script = tempDir.resolve(fileName);
        Files.writeString(script, body, StandardCharsets.UTF_8);
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            script.toFile().setExecutable(true);
        }
        return script.toAbsolutePath().toString();
    }

    private static String successScript() {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return "@echo off\r\n"
                    + "echo {\"suggestions\":{\"MyController\":\"SafeValue\"}}\r\n"
                    + "exit /b 0\r\n";
        }
        return "#!/bin/sh\n"
                + "printf '%s\\n' '{\"suggestions\":{\"MyController\":\"SafeValue\"}}'\n"
                + "exit 0\n";
    }

    private static String failureScript() {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return "@echo off\r\n"
                    + "echo SECRET_OUTPUT_TOKEN\r\n"
                    + "exit /b 17\r\n";
        }
        return "#!/bin/sh\n"
                + "printf '%s\\n' 'SECRET_OUTPUT_TOKEN'\n"
                + "exit 17\n";
    }

    private ClaudeCodeCliAiEnrichmentAdapter newAdapter(final String cliCommand) {
        return new ClaudeCodeCliAiEnrichmentAdapter(
                properties(),
                templateLoader,
                objectMapper,
                cliCommand);
    }

    @Test
    void should_not_log_raw_prompt_or_sanitized_source_on_success() throws IOException {
        String secretSource = "@FXML void onSave() { rawSensitiveSecret(); }";
        String cliCommand = createCliScript("claude-success.cmd", successScript());

        AiEnrichmentResult result = newAdapter(cliCommand).call(
                requestFor(secretSource, "enrichment-default"));

        assertThat(result.degraded()).isFalse();
        assertThat(result.provider()).isEqualTo(LlmProvider.CLAUDE_CODE_CLI);
        assertThat(result.suggestions()).containsEntry("MyController", "SafeValue");

        List<String> messages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
        assertThat(messages).anyMatch(m -> m.contains("promptLength=") && m.contains("promptHash="));
        assertThat(messages).noneMatch(m -> m.contains(secretSource));
        assertThat(messages).noneMatch(m -> m.contains("@FXML void onSave()"));
    }

    @Test
    void should_not_log_raw_process_output_on_failure() throws IOException {
        String cliCommand = createCliScript("claude-failure.cmd", failureScript());

        AiEnrichmentResult result = newAdapter(cliCommand).call(
                requestFor("public class MyController {}", "enrichment-default"));

        assertThat(result.degraded()).isTrue();
        assertThat(result.degradationReason()).contains("exit=17");
        assertThat(result.degradationReason()).doesNotContain("SECRET_OUTPUT_TOKEN");

        List<String> messages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
        assertThat(messages).anyMatch(m -> m.contains("echec")
                && m.contains("exitCode=17")
                && m.contains("outputLength=")
                && m.contains("outputHash="));
        assertThat(messages).noneMatch(m -> m.contains("SECRET_OUTPUT_TOKEN"));
    }
}
