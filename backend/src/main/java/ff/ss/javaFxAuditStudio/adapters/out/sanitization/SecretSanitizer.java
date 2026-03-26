package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.regex.Pattern;

import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationTransformation;

/**
 * Remplace les secrets, URLs internes et tokens par des placeholders (JAS-018).
 *
 * <p>Regles appliquees dans l'ordre :
 * <ol>
 *   <li>Variables nommees apiKey/token/secret/password/credentials → valeur remplacee</li>
 *   <li>Patterns password = "..." → password = "***"</li>
 *   <li>URLs internes → https://INTERNAL_URL</li>
 *   <li>Tokens/API keys (>= 32 chars alphanumeriques isoles) → ***REDACTED***</li>
 * </ol>
 *
 * <p>Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration}.
 */
public class SecretSanitizer implements Sanitizer {

    private static final Pattern NAMED_SECRET_PATTERN = Pattern.compile(
            "(?i)\\b(apiKey|token|secret|password|credentials|api_key)"
            + "\\s*(?:=|:)\\s*\"[^\"]*\"");

    private static final Pattern PASSWORD_ASSIGN_PATTERN = Pattern.compile(
            "(?i)(password\\s*=\\s*)\"[^\"]*\"");

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[^\\s\"']+");

    private static final Pattern LONG_TOKEN_PATTERN = Pattern.compile(
            "\"([A-Za-z0-9]{32,})\"");

    private int occurrenceCount;

    @Override
    public String apply(final String source) {
        occurrenceCount = 0;
        String result = source;

        result = replaceWithCount(result, NAMED_SECRET_PATTERN,
                m -> m.group(1) + " = \"***\"");
        result = replaceWithCount(result, PASSWORD_ASSIGN_PATTERN,
                m -> m.group(1) + "\"***\"");
        result = replaceWithCount(result, URL_PATTERN,
                m -> "https://INTERNAL_URL");
        result = replaceWithCount(result, LONG_TOKEN_PATTERN,
                m -> "\"***REDACTED***\"");

        return result;
    }

    private String replaceWithCount(
            final String source,
            final Pattern pattern,
            final java.util.function.Function<java.util.regex.Matcher, String> replacer) {
        java.util.regex.Matcher matcher = pattern.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb,
                    java.util.regex.Matcher.quoteReplacement(replacer.apply(matcher)));
            occurrenceCount++;
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public SanitizationTransformation report() {
        return new SanitizationTransformation(
                SanitizationRuleType.SECRET_REMOVAL,
                occurrenceCount,
                "Secrets, URLs et tokens remplaces par placeholders");
    }

    @Override
    public SanitizationRuleType ruleType() {
        return SanitizationRuleType.SECRET_REMOVAL;
    }
}
