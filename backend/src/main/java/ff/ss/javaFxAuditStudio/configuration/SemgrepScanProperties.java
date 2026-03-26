package ff.ss.javaFxAuditStudio.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Proprietes de configuration du scan Semgrep post-sanitisation (JAS-018).
 *
 * <p>Liees au prefixe {@code ai.sanitization.semgrep} dans {@code application.properties}.
 * Enregistrees via {@code @EnableConfigurationProperties} dans
 * {@code AiEnrichmentOrchestraConfiguration}.
 *
 * <p>Le scan Semgrep est optionnel : si {@code enabled=false} (defaut) ou si Semgrep
 * n'est pas installe, le sanitizer passe en mode gracieux sans bloquer le pipeline.
 *
 * @param enabled          Active le scan Semgrep (defaut : false)
 * @param cliCommand       Commande CLI Semgrep (defaut : "semgrep")
 * @param timeoutSeconds   Timeout du processus en secondes (defaut : 30)
 * @param failOnFindings   Lance une exception si des findings ERROR sont detectes (defaut : false)
 * @param businessTerms    Termes metier supplementaires a detecter (defaut : liste vide)
 */
@ConfigurationProperties(prefix = "ai.sanitization.semgrep")
public record SemgrepScanProperties(
        boolean enabled,
        String cliCommand,
        int timeoutSeconds,
        boolean failOnFindings,
        List<String> businessTerms) {

    public SemgrepScanProperties {
        cliCommand = (cliCommand != null && !cliCommand.isBlank()) ? cliCommand : "semgrep";
        timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 30;
        if (businessTerms == null) {
            businessTerms = List.of();
        }
    }
}
