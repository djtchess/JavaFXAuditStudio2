package ff.ss.javaFxAuditStudio.adapters.out.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Adaptateur CLI vers OpenAI Codex (abonnement ChatGPT/Codex).
 *
 * <p>Invoque {@code codex exec} en mode non interactif, passe le prompt sur stdin et
 * recupere le message final via {@code --output-last-message}. Aucun credential API
 * n'est requis : l'authentification est geree localement par le CLI Codex.
 */
public class OpenAiCodexCliAiEnrichmentAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiCodexCliAiEnrichmentAdapter.class);
    private static final LlmProvider PROVIDER = LlmProvider.OPENAI_CODEX_CLI;
    private static final HexFormat HEX_FORMAT = HexFormat.of();
    private static final String DEFAULT_CLI_COMMAND = "codex";
    private static final String DEFAULT_MODEL = "gpt-5.3-codex";

    private final AiEnrichmentProperties properties;
    private final PromptTemplateLoader templateLoader;
    private final LlmResponseParser responseParser;
    private final String cliCommand;
    private final String cliModel;

    public OpenAiCodexCliAiEnrichmentAdapter(
            final AiEnrichmentProperties properties,
            final PromptTemplateLoader templateLoader,
            final ObjectMapper objectMapper,
            final String cliCommand,
            final String cliModel) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.templateLoader = Objects.requireNonNull(templateLoader, "templateLoader must not be null");
        this.responseParser = new LlmResponseParser(
                Objects.requireNonNull(objectMapper, "objectMapper must not be null"));
        this.cliCommand = normalizeValue(cliCommand, properties.effectiveCliCommand(PROVIDER), DEFAULT_CLI_COMMAND);
        this.cliModel = normalizeValue(cliModel, properties.effectiveCliModel(PROVIDER), DEFAULT_MODEL);
    }

    public AiEnrichmentResult call(final AiEnrichmentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        SanitizedBundle bundle = request.bundle();
        String prompt = renderPrompt(request);
        Path outputFile = createOutputFile();

        logPreparedPrompt(request, bundle, prompt);
        try {
            return executeCliRequest(request, bundle, prompt, outputFile);
        } finally {
            deleteOutputFileQuietly(outputFile);
        }
    }

    private AiEnrichmentResult executeCliRequest(
            final AiEnrichmentRequest request,
            final SanitizedBundle bundle,
            final String prompt,
            final Path outputFile) {
        try {
            Process process = startProcess(outputFile);
            writePrompt(process, prompt);
            String processOutput = readProcessOutput(process);

            if (!waitForCompletion(process)) {
                return timeoutResult(request, bundle, process);
            }
            if (process.exitValue() != 0) {
                return failureResult(request, bundle, processOutput, process.exitValue());
            }
            return parseCliResponse(request, bundle, outputFile);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOG.warn("OpenAI Codex CLI - interruption requestId={}, controllerRef={}, taskType={}",
                    request.requestId(),
                    bundle.controllerRef(),
                    request.taskType());
            return AiEnrichmentResult.degraded(request.requestId(), "OpenAI Codex CLI interrompu", PROVIDER);
        } catch (IOException exception) {
            LOG.warn("OpenAI Codex CLI - IOException requestId={}, controllerRef={}, taskType={} : {}",
                    request.requestId(),
                    bundle.controllerRef(),
                    request.taskType(),
                    exception.getMessage());
            return AiEnrichmentResult.degraded(
                    request.requestId(),
                    "OpenAI Codex CLI IO: " + exception.getMessage(),
                    PROVIDER);
        }
    }

    private Process startProcess(final Path outputFile) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(outputFile));
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().remove("OPENAI_API_KEY");
        return processBuilder.start();
    }

    private void writePrompt(final Process process, final String prompt) throws IOException {
        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(prompt.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readProcessOutput(final Process process) throws IOException {
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private boolean waitForCompletion(final Process process) throws InterruptedException {
        return process.waitFor(properties.effectiveTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    private AiEnrichmentResult timeoutResult(
            final AiEnrichmentRequest request,
            final SanitizedBundle bundle,
            final Process process) {
        process.destroyForcibly();
        LOG.warn("OpenAI Codex CLI - timeout requestId={}, controllerRef={}, taskType={} apres {}ms",
                request.requestId(),
                bundle.controllerRef(),
                request.taskType(),
                properties.effectiveTimeoutMs());
        return AiEnrichmentResult.degraded(request.requestId(), "OpenAI Codex CLI timeout", PROVIDER);
    }

    private AiEnrichmentResult failureResult(
            final AiEnrichmentRequest request,
            final SanitizedBundle bundle,
            final String processOutput,
            final int exitCode) {
        String outputHash = sha256Hex(processOutput);
        String reason = "OpenAI Codex CLI exit=" + exitCode
                + ", outputLength=" + processOutput.length()
                + ", outputHash=" + outputHash;

        LOG.warn("OpenAI Codex CLI - echec requestId={}, controllerRef={}, taskType={}, exitCode={}, outputLength={}, outputHash={}",
                request.requestId(),
                bundle.controllerRef(),
                request.taskType(),
                exitCode,
                processOutput.length(),
                outputHash);
        return AiEnrichmentResult.degraded(request.requestId(), reason, PROVIDER);
    }

    private AiEnrichmentResult parseCliResponse(
            final AiEnrichmentRequest request,
            final SanitizedBundle bundle,
            final Path outputFile) throws IOException {
        String output = Files.readString(outputFile, StandardCharsets.UTF_8).strip();
        Map<String, String> suggestions = responseParser.parse(
                output,
                bundle.controllerRef(),
                request.requestId());

        if (suggestions.isEmpty()) {
            LOG.warn("OpenAI Codex CLI - reponse non structuree requestId={}, controllerRef={}, taskType={}, outputLength={}, outputHash={}",
                    request.requestId(),
                    bundle.controllerRef(),
                    request.taskType(),
                    output.length(),
                    sha256Hex(output));
            return AiEnrichmentResult.degraded(
                    request.requestId(),
                    "OpenAI Codex CLI reponse non structuree",
                    PROVIDER);
        }

        LOG.info("OpenAI Codex CLI - enrichissement nominal requestId={}, controllerRef={}, taskType={}, model={}",
                request.requestId(),
                bundle.controllerRef(),
                request.taskType(),
                cliModel);
        return new AiEnrichmentResult(request.requestId(), false, "", suggestions, 0, PROVIDER);
    }

    private void logPreparedPrompt(
            final AiEnrichmentRequest request,
            final SanitizedBundle bundle,
            final String prompt) {
        LOG.info("OpenAI Codex CLI - envoi requestId={}, controllerRef={}, taskType={}, model={}, tokens={}",
                request.requestId(),
                bundle.controllerRef(),
                request.taskType(),
                cliModel,
                bundle.estimatedTokens());
        LOG.debug("OpenAI Codex CLI - prompt prepare requestId={}, controllerRef={}, taskType={}, model={}, promptLength={}, promptHash={}",
                request.requestId(),
                bundle.controllerRef(),
                request.taskType(),
                cliModel,
                prompt.length(),
                sha256Hex(prompt));
    }

    private Path createOutputFile() {
        try {
            return Files.createTempFile("codex-ai-enrichment-", ".txt");
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de creer le fichier de sortie Codex CLI", exception);
        }
    }

    private void deleteOutputFileQuietly(final Path outputFile) {
        if (outputFile != null) {
            try {
                Files.deleteIfExists(outputFile);
            } catch (IOException exception) {
                LOG.debug("OpenAI Codex CLI - suppression ignoree du fichier temporaire {}", outputFile);
            }
        }
    }

    private String renderPrompt(final AiEnrichmentRequest request) {
        Map<String, Object> context = PromptContextBudgetSupport.budgetContext(properties, request);
        String dataPrompt = templateLoader.render(request.promptTemplate(), context);
        return "# SYSTEM\n" + StructuredOutputContract.strictSystemPrompt(
                request.bundle().controllerRef(),
                request.taskType()) + "\n\n"
                + "# DATA\n[DATA START]\n" + dataPrompt + "\n[DATA END]";
    }

    private List<String> buildCommand(final Path outputFile) {
        if (isWindows()) {
            return List.of(
                    "cmd",
                    "/c",
                    cliCommand,
                    "exec",
                    "--skip-git-repo-check",
                    "--sandbox",
                    "read-only",
                    "--color",
                    "never",
                    "--model",
                    cliModel,
                    "--output-last-message",
                    outputFile.toString(),
                    "-");
        }
        return List.of(
                cliCommand,
                "exec",
                "--skip-git-repo-check",
                "--sandbox",
                "read-only",
                "--color",
                "never",
                "--model",
                cliModel,
                "--output-last-message",
                outputFile.toString(),
                "-");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    private static String normalizeValue(final String value, final String fallback, final String secondaryFallback) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return secondaryFallback;
    }

    private static String sha256Hex(final String value) {
        String content = value != null ? value : "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HEX_FORMAT.formatHex(hashBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }
}
