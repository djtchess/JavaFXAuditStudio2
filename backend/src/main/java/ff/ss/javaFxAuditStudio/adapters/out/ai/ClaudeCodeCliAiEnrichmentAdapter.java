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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Adaptateur CLI vers Claude Code (abonnement).
 *
 * <p>Invoque {@code claude --print} via ProcessBuilder et passe le prompt sur stdin.
 * La reponse est lue sur stdout et parsee par {@link LlmResponseParser}.
 * Aucun credential API n'est requis - l'authentification est geree par Claude Code CLI.
 *
 * <p>Assemble via {@code AiEnrichmentOrchestraConfiguration} - pas de {@code @Component}.
 */
public class ClaudeCodeCliAiEnrichmentAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ClaudeCodeCliAiEnrichmentAdapter.class);
    private static final LlmProvider PROVIDER = LlmProvider.CLAUDE_CODE_CLI;
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final AiEnrichmentProperties properties;
    private final PromptTemplateLoader templateLoader;
    private final LlmResponseParser responseParser;
    private final String cliCommand;

    public ClaudeCodeCliAiEnrichmentAdapter(
            final AiEnrichmentProperties properties,
            final PromptTemplateLoader templateLoader,
            final ObjectMapper objectMapper,
            final String cliCommand) {
        this.properties = Objects.requireNonNull(properties);
        this.templateLoader = Objects.requireNonNull(templateLoader);
        this.responseParser = new LlmResponseParser(objectMapper);
        this.cliCommand = (cliCommand != null && !cliCommand.isBlank()) ? cliCommand : "claude";
    }

    public AiEnrichmentResult call(final AiEnrichmentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        SanitizedBundle bundle = request.bundle();
        String prompt = renderPrompt(request);

        LOG.info("Claude CLI - envoi requestId={}, controllerRef={}, taskType={}, tokens={}",
                request.requestId(), bundle.controllerRef(), request.taskType(),
                bundle.estimatedTokens());
        LOG.debug(
                "Claude CLI - prompt prepare requestId={}, controllerRef={}, taskType={}, promptLength={}, promptHash={}",
                request.requestId(),
                bundle.controllerRef(),
                request.taskType(),
                prompt.length(),
                sha256Hex(prompt));

        try {
            ProcessBuilder pb = new ProcessBuilder(buildCommand(cliCommand));
            // Fusionner stderr dans stdout : evite tout risque de deadlock et capture
            // les messages d'erreur que le CLI ecrit indifferemment sur l'un ou l'autre flux.
            pb.redirectErrorStream(true);
            // Supprimer les variables d'API key du subprocess : le CLI doit utiliser ses
            // propres credentials (claude auth login), pas une cle heritee du process parent.
            pb.environment().remove("ANTHROPIC_API_KEY");
            pb.environment().remove("CLAUDE_API_KEY");
            Process process = pb.start();

            // Envoyer le prompt sur stdin puis fermer le flux pour signaler EOF au CLI
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(prompt.getBytes(StandardCharsets.UTF_8));
            }

            // Lire l'integralite de la sortie (stdout + stderr fusionne) avant waitFor
            String output = new String(
                    process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            boolean finished = process.waitFor(
                    properties.effectiveTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOG.warn("Claude CLI - timeout requestId={}, controllerRef={}, taskType={} apres {}ms",
                        request.requestId(), bundle.controllerRef(), request.taskType(),
                        properties.effectiveTimeoutMs());
                return AiEnrichmentResult.degraded(
                        request.requestId(), "Claude CLI timeout", PROVIDER);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String outputHash = sha256Hex(output);
                String reason = "Claude CLI exit=" + exitCode
                        + ", outputLength=" + output.length()
                        + ", outputHash=" + outputHash;
                LOG.warn(
                        "Claude CLI - echec requestId={}, controllerRef={}, taskType={}, exitCode={}, outputLength={}, outputHash={}",
                        request.requestId(),
                        bundle.controllerRef(),
                        request.taskType(),
                        exitCode,
                        output.length(),
                        outputHash);
                return AiEnrichmentResult.degraded(request.requestId(), reason, PROVIDER);
            }

            Map<String, String> suggestions = responseParser.parse(
                    output.strip(), bundle.controllerRef(), request.requestId());

            LOG.info("Claude CLI - enrichissement nominal requestId={}, controllerRef={}, taskType={}",
                    request.requestId(), bundle.controllerRef(), request.taskType());
            return new AiEnrichmentResult(
                    request.requestId(), false, "", suggestions, 0, PROVIDER);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Claude CLI - interruption requestId={}, controllerRef={}, taskType={}",
                    request.requestId(), bundle.controllerRef(), request.taskType());
            return AiEnrichmentResult.degraded(
                    request.requestId(), "Claude CLI interrompu", PROVIDER);
        } catch (IOException e) {
            LOG.warn("Claude CLI - IOException requestId={}, controllerRef={}, taskType={} : {}",
                    request.requestId(), bundle.controllerRef(), request.taskType(), e.getMessage());
            return AiEnrichmentResult.degraded(
                    request.requestId(), "Claude CLI IO: " + e.getMessage(), PROVIDER);
        }
    }

    private String renderPrompt(final AiEnrichmentRequest request) {
        Map<String, Object> context = new java.util.HashMap<>();
        context.put("controllerRef", request.bundle().controllerRef());
        context.put("sanitizedSource", request.bundle().sanitizedSource());
        context.put("estimatedTokens", request.bundle().estimatedTokens());
        context.put("taskType", request.taskType().name());
        context.putAll(request.extraContext());
        return templateLoader.render(request.promptTemplate(), context);
    }

    private static String sha256Hex(final String value) {
        String content = value != null ? value : "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HEX_FORMAT.formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Construit la commande ProcessBuilder selon l'OS.
     * Sur Windows, ProcessBuilder ne resout pas les .cmd sans passer par cmd.exe.
     */
    private static List<String> buildCommand(final String cliCmd) {
        if (System.getProperty("os.name", "").toLowerCase().contains("windows")) {
            return List.of("cmd", "/c", cliCmd, "--print");
        }
        return List.of(cliCmd, "--print");
    }
}
