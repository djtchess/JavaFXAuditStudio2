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

class OpenAiCodexCliAiEnrichmentAdapterTest {

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
        logger = (Logger) LoggerFactory.getLogger(OpenAiCodexCliAiEnrichmentAdapter.class);
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
                "openai-codex-cli",
                5_000L,
                null,
                null,
                false,
                null,
                "gpt-5.3-codex",
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
        if (isWindows()) {
            return "@echo off\r\n"
                    + "setlocal EnableDelayedExpansion\r\n"
                    + "set \"OUT=\"\r\n"
                    + ":loop\r\n"
                    + "if \"%~1\"==\"\" goto end\r\n"
                    + "if \"%~1\"==\"--output-last-message\" (\r\n"
                    + "  set \"OUT=%~2\"\r\n"
                    + "  shift\r\n"
                    + ")\r\n"
                    + "shift\r\n"
                    + "goto loop\r\n"
                    + ":end\r\n"
                    + "> \"%OUT%\" echo {\"suggestions\":{\"MyController\":\"SafeValue\"}}\r\n"
                    + "exit /b 0\r\n";
        }
        return "#!/bin/sh\n"
                + "OUT=\"\"\n"
                + "while [ \"$#\" -gt 0 ]; do\n"
                + "  if [ \"$1\" = \"--output-last-message\" ]; then\n"
                + "    OUT=\"$2\"\n"
                + "    shift 2\n"
                + "  else\n"
                + "    shift\n"
                + "  fi\n"
                + "done\n"
                + "printf '%s\\n' '{\"suggestions\":{\"MyController\":\"SafeValue\"}}' > \"$OUT\"\n"
                + "exit 0\n";
    }

    private static String invalidJsonScript() {
        if (isWindows()) {
            return "@echo off\r\n"
                    + "setlocal EnableDelayedExpansion\r\n"
                    + "set \"OUT=\"\r\n"
                    + ":loop\r\n"
                    + "if \"%~1\"==\"\" goto end\r\n"
                    + "if \"%~1\"==\"--output-last-message\" (\r\n"
                    + "  set \"OUT=%~2\"\r\n"
                    + "  shift\r\n"
                    + ")\r\n"
                    + "shift\r\n"
                    + "goto loop\r\n"
                    + ":end\r\n"
                    + "> \"%OUT%\" echo SavePatientUseCase\r\n"
                    + "exit /b 0\r\n";
        }
        return "#!/bin/sh\n"
                + "OUT=\"\"\n"
                + "while [ \"$#\" -gt 0 ]; do\n"
                + "  if [ \"$1\" = \"--output-last-message\" ]; then\n"
                + "    OUT=\"$2\"\n"
                + "    shift 2\n"
                + "  else\n"
                + "    shift\n"
                + "  fi\n"
                + "done\n"
                + "printf '%s\\n' 'SavePatientUseCase' > \"$OUT\"\n"
                + "exit 0\n";
    }

    private static String failureScript() {
        if (isWindows()) {
            return "@echo off\r\n"
                    + "echo SECRET_OUTPUT_TOKEN\r\n"
                    + "exit /b 17\r\n";
        }
        return "#!/bin/sh\n"
                + "printf '%s\\n' 'SECRET_OUTPUT_TOKEN'\n"
                + "exit 17\n";
    }

    private OpenAiCodexCliAiEnrichmentAdapter newAdapter(final String cliCommand) {
        return new OpenAiCodexCliAiEnrichmentAdapter(
                properties(),
                templateLoader,
                objectMapper,
                cliCommand,
                "gpt-5.3-codex");
    }

    @Test
    void should_not_log_raw_prompt_or_sanitized_source_on_success() throws IOException {
        String secretSource = "@FXML void onSave() { rawSensitiveSecret(); }";
        String cliCommand = createCliScript("codex-success.cmd", successScript());

        AiEnrichmentResult result = newAdapter(cliCommand).call(
                requestFor(secretSource, "enrichment-default"));

        assertThat(result.degraded()).isFalse();
        assertThat(result.provider()).isEqualTo(LlmProvider.OPENAI_CODEX_CLI);
        assertThat(result.suggestions()).containsEntry("MyController", "SafeValue");

        List<String> messages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
        assertThat(messages).anyMatch(message ->
                message.contains("promptLength=") && message.contains("promptHash="));
        assertThat(messages).noneMatch(message -> message.contains(secretSource));
        assertThat(messages).noneMatch(message -> message.contains("@FXML void onSave()"));
    }

    @Test
    void should_not_log_raw_process_output_on_failure() throws IOException {
        String cliCommand = createCliScript("codex-failure.cmd", failureScript());

        AiEnrichmentResult result = newAdapter(cliCommand).call(
                requestFor("public class MyController {}", "enrichment-default"));

        assertThat(result.degraded()).isTrue();
        assertThat(result.degradationReason()).contains("exit=17");
        assertThat(result.degradationReason()).doesNotContain("SECRET_OUTPUT_TOKEN");

        List<String> messages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
        assertThat(messages).anyMatch(message -> message.contains("echec")
                && message.contains("exitCode=17")
                && message.contains("outputLength=")
                && message.contains("outputHash="));
        assertThat(messages).noneMatch(message -> message.contains("SECRET_OUTPUT_TOKEN"));
    }

    @Test
    void should_degrade_when_cli_output_is_not_valid_json() throws IOException {
        String cliCommand = createCliScript("codex-invalid.cmd", invalidJsonScript());

        AiEnrichmentResult result = newAdapter(cliCommand).call(
                requestFor("public class MyController {}", "enrichment-default"));

        assertThat(result.degraded()).isTrue();
        assertThat(result.provider()).isEqualTo(LlmProvider.OPENAI_CODEX_CLI);
        assertThat(result.degradationReason()).contains("non structuree");
        assertThat(result.suggestions()).isEmpty();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
