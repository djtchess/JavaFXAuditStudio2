package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.configuration.SemgrepScanProperties;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationTransformation;
import ff.ss.javaFxAuditStudio.domain.sanitization.SemgrepFinding;

/**
 * Sanitizer Semgrep post-pipeline (JAS-018).
 *
 * <p>Scanne le source sanitise avec Semgrep pour detecter des secrets ou URLs
 * residuels que les sanitizers regex auraient manques. Ne modifie jamais le source :
 * il scanne uniquement et retourne le source inchange.
 *
 * <p>Mode gracieux : si Semgrep n'est pas installe ou si le processus depasse le
 * timeout, un WARN est emis et le source passe sans blocage.
 *
 * <p>Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration}.
 */
public class SemgrepScanSanitizer implements Sanitizer {

    private static final Logger LOG = LoggerFactory.getLogger(SemgrepScanSanitizer.class);

    private static final String SEVERITY_ERROR = "ERROR";

    private final SemgrepScanProperties properties;
    private final ObjectMapper objectMapper;

    private int occurrenceCount;

    public SemgrepScanSanitizer(
            final SemgrepScanProperties properties,
            final ObjectMapper objectMapper) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public String apply(final String source) {
        Objects.requireNonNull(source, "source must not be null");
        occurrenceCount = 0;

        if (!properties.enabled()) {
            LOG.debug("Scan Semgrep desactive — source transmis sans modification");
            return source;
        }

        Path sourceFile = null;
        Path rulesFile = null;
        try {
            sourceFile = Files.createTempFile("semgrep_src_", ".java");
            rulesFile = Files.createTempFile("semgrep_rules_", ".yaml");

            Files.writeString(sourceFile, source, StandardCharsets.UTF_8);
            Files.writeString(rulesFile, buildRulesYaml(), StandardCharsets.UTF_8);

            List<SemgrepFinding> findings = runSemgrep(sourceFile, rulesFile);
            occurrenceCount = findings.size();
            logFindings(findings);

            boolean hasErrorFindings = findings.stream()
                    .anyMatch(f -> SEVERITY_ERROR.equalsIgnoreCase(f.severity()));
            if (properties.failOnFindings() && hasErrorFindings) {
                throw new SanitizationRefusedException(
                        "Scan Semgrep : " + occurrenceCount
                        + " finding(s) de severite ERROR detecte(s) apres sanitisation");
            }

        } catch (SanitizationRefusedException e) {
            throw e;
        } catch (IOException e) {
            LOG.warn("Scan Semgrep indisponible (IOException) — passage en mode gracieux : {}",
                    e.getMessage());
            occurrenceCount = 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Scan Semgrep interrompu — passage en mode gracieux");
            occurrenceCount = 0;
        } catch (Exception e) {
            LOG.warn("Erreur inattendue lors du scan Semgrep — passage en mode gracieux : {}",
                    e.getMessage());
            occurrenceCount = 0;
        } finally {
            deleteSilently(sourceFile);
            deleteSilently(rulesFile);
        }

        return source;
    }

    @Override
    public SanitizationTransformation report() {
        return new SanitizationTransformation(
                SanitizationRuleType.SEMGREP_SECURITY_SCAN,
                occurrenceCount,
                "Semgrep : " + occurrenceCount + " finding(s) detecte(s)");
    }

    @Override
    public SanitizationRuleType ruleType() {
        return SanitizationRuleType.SEMGREP_SECURITY_SCAN;
    }

    // --- Methodes privees ---

    private List<SemgrepFinding> runSemgrep(
            final Path sourceFile, final Path rulesFile)
            throws IOException, InterruptedException {

        List<String> command = buildCommand(sourceFile, rulesFile);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            LOG.warn("Semgrep non trouve sur PATH (commande : {}) — mode gracieux",
                    properties.cliCommand());
            return List.of();
        }

        // Lecture de stdout et stderr en parallele pour eviter les deadlocks
        CompletableFuture<String> stdoutFuture = readStreamAsync(process.getInputStream());
        CompletableFuture<String> stderrFuture = readStreamAsync(process.getErrorStream());

        boolean finished = process.waitFor(properties.timeoutSeconds(), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            LOG.warn("Scan Semgrep depasse le timeout de {}s — mode gracieux",
                    properties.timeoutSeconds());
            return List.of();
        }

        String stdout = stdoutFuture.join();
        String stderr = stderrFuture.join();
        int exitCode = process.exitValue();

        // exit 0 = aucun finding, exit 1 = findings, exit >=2 = erreur Semgrep
        if (exitCode >= 2) {
            LOG.warn("Semgrep a termine avec code d'erreur {} — mode gracieux. Stderr: {}",
                    exitCode, stderr.isBlank() ? "(vide)" : stderr.trim());
            return List.of();
        }

        return parseFindings(stdout);
    }

    private List<String> buildCommand(final Path sourceFile, final Path rulesFile) {
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> command = new ArrayList<>();
        if (os.contains("win")) {
            command.add("cmd");
            command.add("/c");
        }
        command.add(properties.cliCommand());
        command.add("scan");
        command.add("--config");
        command.add(rulesFile.toAbsolutePath().toString());
        command.add("--json");
        command.add("--timeout");
        command.add(String.valueOf(properties.timeoutSeconds()));
        command.add("--no-git-ignore");
        command.add("--metrics=off");
        command.add("--quiet");
        command.add(sourceFile.toAbsolutePath().toString());
        return command;
    }

    private CompletableFuture<String> readStreamAsync(final InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOG.debug("Erreur lecture stream Semgrep : {}", e.getMessage());
                return "";
            }
        });
    }

    private List<SemgrepFinding> parseFindings(final String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) {
                return List.of();
            }
            List<SemgrepFinding> findings = new ArrayList<>();
            for (JsonNode result : results) {
                String ruleId = textOrEmpty(result, "check_id");
                int line = 0;
                JsonNode startNode = result.get("start");
                if (startNode != null && startNode.has("line")) {
                    line = startNode.get("line").asInt(0);
                }
                String severity = "";
                String message = "";
                String snippet = "";
                JsonNode extra = result.get("extra");
                if (extra != null) {
                    severity = textOrEmpty(extra, "severity");
                    message = textOrEmpty(extra, "message");
                    snippet = textOrEmpty(extra, "lines");
                }
                findings.add(new SemgrepFinding(ruleId, line, severity, message, snippet));
            }
            return findings;
        } catch (Exception e) {
            LOG.warn("Impossible de parser la sortie JSON de Semgrep : {}", e.getMessage());
            return List.of();
        }
    }

    private void logFindings(final List<SemgrepFinding> findings) {
        for (SemgrepFinding finding : findings) {
            if (SEVERITY_ERROR.equalsIgnoreCase(finding.severity())) {
                LOG.warn("Semgrep [{}] ligne {} — {} : {}",
                        finding.ruleId(), finding.line(), finding.severity(), finding.message());
            } else {
                LOG.info("Semgrep [{}] ligne {} — {} : {}",
                        finding.ruleId(), finding.line(), finding.severity(), finding.message());
            }
        }
    }

    private String buildRulesYaml() {
        StringBuilder yaml = new StringBuilder();
        yaml.append("rules:\n");
        yaml.append("  - id: hardcoded-secret\n");
        yaml.append("    languages: [java]\n");
        yaml.append("    message: \"Secret potentiellement hardcod\u00e9 d\u00e9tect\u00e9.\"\n");
        yaml.append("    severity: ERROR\n");
        yaml.append("    pattern-regex: '(?i)(password|passwd|pwd|secret|apiKey|api_key|token|accessKey|access_key|privateKey)\\s*=\\s*\"[^\"]{4,}\"'\n");
        yaml.append("  - id: internal-ip-url\n");
        yaml.append("    languages: [java]\n");
        yaml.append("    message: \"URL r\u00e9seau priv\u00e9 RFC1918 d\u00e9tect\u00e9e.\"\n");
        yaml.append("    severity: WARNING\n");
        yaml.append("    pattern-regex: 'https?://(10\\.\\d+\\.\\d+\\.\\d+|172\\.(1[6-9]|2\\d|3[01])\\.\\d+\\.\\d+|192\\.168\\.\\d+\\.\\d+)[:/\"]'\n");
        yaml.append("  - id: hardcoded-url\n");
        yaml.append("    languages: [java]\n");
        yaml.append("    message: \"URL HTTP hardcod\u00e9e d\u00e9tect\u00e9e.\"\n");
        yaml.append("    severity: WARNING\n");
        yaml.append("    pattern-regex: '\"https?://[a-zA-Z0-9._-]+\\.[a-zA-Z]{2,}[^\"]*\"'\n");

        List<String> terms = properties.businessTerms();
        if (terms != null && !terms.isEmpty()) {
            String regex = String.join("|", terms);
            yaml.append("  - id: business-term\n");
            yaml.append("    languages: [java]\n");
            yaml.append("    message: \"Terme m\u00e9tier sensible d\u00e9tect\u00e9.\"\n");
            yaml.append("    severity: INFO\n");
            yaml.append("    pattern-regex: '\"[^\"]*(" + regex + ")[^\"]*\"'\n");
        }

        return yaml.toString();
    }

    private static String textOrEmpty(final JsonNode node, final String field) {
        JsonNode child = node.get(field);
        return (child != null && !child.isNull()) ? child.asText("") : "";
    }

    private static void deleteSilently(final Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                LOG.debug("Impossible de supprimer le fichier temporaire {} : {}", path, e.getMessage());
            }
        }
    }
}
