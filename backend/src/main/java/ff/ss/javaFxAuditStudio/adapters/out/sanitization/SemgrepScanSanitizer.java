package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.configuration.SemgrepScanProperties;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizableFile;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationTransformation;
import ff.ss.javaFxAuditStudio.domain.sanitization.SemgrepFinding;

/**
 * Sanitizer Semgrep post-pipeline (JAS-018 / QW-5 / AI-3).
 *
 * <p>Scanne le source sanitise avec Semgrep pour detecter des secrets ou URLs
 * residuels que les sanitizers regex auraient manques. Ne modifie jamais le source :
 * il scanne uniquement et retourne le source inchange.
 *
 * <p>Les regles statiques (Java) sont chargees depuis {@code semgrep/sanitization-rules.yaml}
 * sur le classpath. En cas d'absence de la ressource, un fallback inline est utilise.
 * Les regles dynamiques ({@code business-term}, {@code denylist-term}) et les regles
 * non-Java ({@code properties-secret}, {@code yaml-secret}, {@code generic-url})
 * sont ajoutees a la volee.
 *
 * <p>Mode gracieux : si Semgrep n'est pas installe ou si le processus depasse le
 * timeout, un WARN est emis et le source passe sans blocage.
 *
 * <p>Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration}.
 */
public class SemgrepScanSanitizer implements Sanitizer {

    private static final Logger LOG = LoggerFactory.getLogger(SemgrepScanSanitizer.class);

    private static final String SEVERITY_ERROR = "ERROR";
    private static final String RULES_CLASSPATH = "/semgrep/sanitization-rules.yaml";

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
        return applyWithExtension(source, "java");
    }

    /**
     * Applique le scan Semgrep sur un fichier non-Java identifie par son type.
     *
     * <p>Cree le fichier temporaire avec l'extension correcte selon le {@code fileType},
     * ce qui permet a Semgrep d'utiliser les regles {@code languages: [generic]}.
     * Retourne le source sans modification (scan uniquement).
     *
     * @param file fichier candidat (non null)
     * @return source inchange apres scan
     */
    public String applyToFile(final SanitizableFile file) {
        Objects.requireNonNull(file, "file must not be null");
        return applyWithExtension(file.content(), file.fileType());
    }

    String applyWithExtension(final String source, final String fileExtension) {
        Objects.requireNonNull(source, "source must not be null");
        occurrenceCount = 0;

        if (!properties.enabled()) {
            LOG.debug("Scan Semgrep desactive — source transmis sans modification");
            return source;
        }

        Path sourceFile = null;
        Path rulesFile = null;
        try {
            sourceFile = Files.createTempFile("semgrep_src_", "." + fileExtension);
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

    // --- Construction des regles YAML ---

    /**
     * Construit le YAML complet des regles Semgrep :
     * <ol>
     *   <li>Regles Java statiques : chargees depuis le classpath ou fallback inline.</li>
     *   <li>Regles non-Java generiques ({@code properties-secret}, {@code yaml-secret},
     *       {@code generic-url}) — toujours ajoutees.</li>
     *   <li>Regles dynamiques : {@code business-term} et {@code denylist-term} selon config.</li>
     * </ol>
     */
    String buildRulesYaml() {
        Optional<String> classpathYaml = loadRulesFromClasspath();
        String baseYaml = classpathYaml.orElseGet(this::buildRulesYamlFallback);

        StringBuilder combined = new StringBuilder(baseYaml);
        appendNonJavaRules(combined);
        appendBusinessTermRule(combined);
        appendDenylistRule(combined);

        return combined.toString();
    }

    /**
     * Tente de charger les regles statiques depuis le classpath.
     *
     * @return le contenu YAML ou {@code Optional.empty()} si la ressource est absente
     */
    Optional<String> loadRulesFromClasspath() {
        try (InputStream stream = getClass().getResourceAsStream(RULES_CLASSPATH)) {
            if (stream == null) {
                LOG.debug("Ressource classpath {} absente — utilisation du fallback inline",
                        RULES_CLASSPATH);
                return Optional.empty();
            }
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            LOG.debug("Regles Semgrep chargees depuis le classpath ({})", RULES_CLASSPATH);
            return Optional.of(content);
        } catch (IOException e) {
            LOG.warn("Erreur lecture ressource classpath {} — fallback inline : {}",
                    RULES_CLASSPATH, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Regles Java inline de secours, identiques au fichier YAML statique.
     * Utilisees si la ressource classpath est absente.
     */
    String buildRulesYamlFallback() {
        StringBuilder yaml = new StringBuilder();
        yaml.append("rules:\n");
        appendStaticRule(yaml, "hardcoded-secret", "[java]", "ERROR",
                "Secret potentiellement hardcode detecte.",
                "(?i)(password|passwd|pwd|secret|apiKey|api_key|token|accessKey|access_key|privateKey)\\s*=\\s*\"[^\"]{4,}\"");
        appendStaticRule(yaml, "internal-ip-url", "[java]", "WARNING",
                "URL reseau prive RFC1918 detectee.",
                "https?://(10\\.\\d+\\.\\d+\\.\\d+|172\\.(1[6-9]|2\\d|3[01])\\.\\d+\\.\\d+|192\\.168\\.\\d+\\.\\d+)[:/\"]");
        appendStaticRule(yaml, "hardcoded-url", "[java]", "WARNING",
                "URL HTTP hardcodee detectee.",
                "\"https?://[a-zA-Z0-9._-]+\\.[a-zA-Z]{2,}[^\"]*\"");
        appendStaticRule(yaml, "internal-host-url", "[java]", "WARNING",
                "URL interne ou corporate detectee.",
                "\"https?://[a-zA-Z0-9._-]+\\.(internal|intra|corp|lan)[^\"]*\"");
        appendStaticRule(yaml, "jdbc-connection-string", "[java]", "WARNING",
                "Chaine de connexion JDBC detectee.",
                "\"jdbc:(postgresql|mysql|oracle|sqlserver|h2):[^\"]*\"");
        appendStaticRule(yaml, "private-key-marker", "[java]", "ERROR",
                "Marqueur de cle privee detecte.",
                "\"-----BEGIN (RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----\"");
        return yaml.toString();
    }

    // --- Methodes privees ---

    private void appendNonJavaRules(final StringBuilder yaml) {
        appendStaticRule(yaml, "properties-secret", "[generic]", "ERROR",
                "Secret detecte dans fichier properties.",
                "(?i)^\\s*(password|secret|api\\.key|api_key|token)\\s*=\\s*\\S{4,}");
        appendStaticRule(yaml, "yaml-secret", "[generic]", "ERROR",
                "Secret detecte dans fichier YAML.",
                "(?i)^\\s*(password|secret|api-key|api_key|token)\\s*:\\s*\\S{4,}");
        appendStaticRule(yaml, "generic-url", "[generic]", "WARNING",
                "URL HTTP detectee dans fichier non-Java.",
                "https?://[a-zA-Z0-9._-]+\\.[a-zA-Z]{2,}");
        appendStaticRule(yaml, "generic-jdbc-url", "[generic]", "WARNING",
                "Chaine JDBC detectee dans fichier non-Java.",
                "jdbc:(postgresql|mysql|oracle|sqlserver|h2):\\S+");
        appendStaticRule(yaml, "generic-private-key-marker", "[generic]", "ERROR",
                "Marqueur de cle privee detecte dans fichier non-Java.",
                "-----BEGIN (RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----");
    }

    private void appendStaticRule(
            final StringBuilder yaml, final String id, final String languages,
            final String severity, final String message, final String regex) {
        yaml.append("  - id: ").append(id).append('\n');
        yaml.append("    languages: ").append(languages).append('\n');
        yaml.append("    message: \"").append(message).append("\"\n");
        yaml.append("    severity: ").append(severity).append('\n');
        yaml.append("    pattern-regex: '").append(regex).append("'\n");
    }

    private void appendBusinessTermRule(final StringBuilder yaml) {
        List<String> terms = properties.businessTerms();
        if (terms == null || terms.isEmpty()) {
            return;
        }
        String regex = buildSafeRegex(terms);
        yaml.append("  - id: business-term\n");
        yaml.append("    languages: [java]\n");
        yaml.append("    message: \"Terme metier sensible detecte.\"\n");
        yaml.append("    severity: INFO\n");
        yaml.append("    pattern-regex: '\"[^\"]*(" + regex + ")[^\"]*\"'\n");
    }

    private void appendDenylistRule(final StringBuilder yaml) {
        List<String> terms = properties.denylist();
        if (terms == null || terms.isEmpty()) {
            return;
        }
        String regex = buildSafeRegex(terms);
        yaml.append("  - id: denylist-term\n");
        yaml.append("    languages: [java]\n");
        yaml.append("    message: \"Terme bloque par la denylist detecte.\"\n");
        yaml.append("    severity: WARNING\n");
        yaml.append("    pattern-regex: '\"[^\"]*(" + regex + ")[^\"]*\"'\n");
    }

    /**
     * Echappe chaque terme avec {@link Pattern#quote} pour prevenir
     * l'injection de regex malveillante via la configuration.
     */
    private static String buildSafeRegex(final List<String> terms) {
        return terms.stream()
                .map(Pattern::quote)
                .reduce((a, b) -> a + "|" + b)
                .orElse("");
    }

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
                findings.add(parseSingleFinding(result));
            }
            return findings;
        } catch (Exception e) {
            LOG.warn("Impossible de parser la sortie JSON de Semgrep : {}", e.getMessage());
            return List.of();
        }
    }

    private SemgrepFinding parseSingleFinding(final JsonNode result) {
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
        return new SemgrepFinding(ruleId, line, severity, message, snippet);
    }

    private void logFindings(final List<SemgrepFinding> findings) {
        for (SemgrepFinding finding : findings) {
            if (SEVERITY_ERROR.equalsIgnoreCase(finding.severity())) {
                LOG.warn("Semgrep [{}] ligne {} — {}",
                        finding.ruleId(), finding.line(), finding.severity());
            } else {
                LOG.info("Semgrep [{}] ligne {} — {}",
                        finding.ruleId(), finding.line(), finding.severity());
            }
        }
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
