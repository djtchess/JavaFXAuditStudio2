package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detecteur de marqueurs sensibles residuels apres sanitisation (JAS-018).
 *
 * <p>Utilise apres application de tous les sanitizers pour verifier
 * qu'aucun marqueur sensible ne subsiste dans la source sanitisee.
 * Retourne {@code true} si au moins un marqueur est detecte — dans ce cas
 * l'envoi au LLM doit etre bloque.
 *
 * <p>Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration}.
 */
public class SensitiveMarkerDetector {

    private static final Logger LOG = LoggerFactory.getLogger(SensitiveMarkerDetector.class);

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[^\\s\"']+");

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    // Chaine alphanum de plus de 30 chars SANS transition camelCase — potentiel token ou secret.
    // Les identifiants Java camelCase (ex: DonneesEntretienMedicalService) sont exclus car
    // ils contiennent au moins une transition minuscule->majuscule.
    private static final Pattern LONG_ALNUM_PATTERN = Pattern.compile(
            "[A-Za-z0-9]{31,}");

    // Detecte les chaines camelCase (au moins une transition minuscule->majuscule)
    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile(
            "[a-z][A-Z]");

    private static final Pattern SENSITIVE_KEYWORDS = Pattern.compile(
            "(?i)\\b(password|secret|token|apiKey|credentials|api_key)\\b");

    /**
     * Verifie si la source sanitisee contient encore des marqueurs sensibles.
     *
     * @param sanitizedSource source apres application du pipeline de sanitisation
     * @return {@code true} si au moins un marqueur sensible est detecte, {@code false} sinon
     */
    public boolean hasSensitiveMarkers(final String sanitizedSource) {
        if (sanitizedSource == null || sanitizedSource.isBlank()) {
            return false;
        }
        if (URL_PATTERN.matcher(sanitizedSource).find()) {
            LOG.info("Marqueur sensible detecte : URL presente dans la source sanitisee");
            return true;
        }
        if (EMAIL_PATTERN.matcher(sanitizedSource).find()) {
            LOG.info("Marqueur sensible detecte : adresse email presente dans la source sanitisee");
            return true;
        }
        if (hasNonCamelCaseLongToken(sanitizedSource)) {
            return true;
        }
        if (SENSITIVE_KEYWORDS.matcher(sanitizedSource).find()) {
            LOG.info("Marqueur sensible detecte : mot-cle sensible (password/secret/token/...) present");
            return true;
        }
        return false;
    }

    /**
     * Verifie la presence d'une chaine alphanum de 31+ caracteres qui ne ressemble PAS
     * a un identifiant Java camelCase. Les noms de classes/methodes Java sont exclus
     * (ils contiennent une transition minuscule->majuscule). Seules les chaines sans
     * camelCase (tokens, cles base64, secrets) declenchent le rejet.
     */
    private boolean hasNonCamelCaseLongToken(final String source) {
        Matcher m = LONG_ALNUM_PATTERN.matcher(source);
        while (m.find()) {
            String candidate = m.group();
            if (!CAMEL_CASE_PATTERN.matcher(candidate).find()) {
                LOG.info("Marqueur sensible detecte : chaine alphanum longue sans camelCase "
                        + "(potentiel token/secret) : {}...", candidate.substring(0, Math.min(12, candidate.length())));
                return true;
            }
        }
        return false;
    }
}
