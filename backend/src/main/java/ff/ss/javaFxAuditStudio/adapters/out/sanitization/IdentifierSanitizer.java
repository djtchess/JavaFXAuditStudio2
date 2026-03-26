package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationTransformation;

/**
 * Remplace les identifiants Java metier par des termes generiques (JAS-018).
 *
 * <p>Cible les noms de classes et variables contenant des termes metier reconnaissables
 * definis dans {@link BusinessTermDictionary}.
 * Les identifiants standards Java (List, String, Object…) ne sont pas affectes.
 *
 * <p>Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration}.
 */
public class IdentifierSanitizer implements Sanitizer {

    /**
     * Pattern issu de {@link BusinessTermDictionary} : source unique de verite
     * pour les suffixes metier reconnus (QW-2).
     */
    private static final Pattern BUSINESS_SUFFIX_PATTERN =
            BusinessTermDictionary.BUSINESS_IDENTIFIER_PATTERN;

    private int occurrenceCount;

    @Override
    public String apply(final String source) {
        occurrenceCount = 0;
        final Map<String, String> replacements = new HashMap<>();
        final AtomicInteger counter = new AtomicInteger(1);

        Matcher matcher = BUSINESS_SUFFIX_PATTERN.matcher(source);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String original = matcher.group();
            String replacement = replacements.computeIfAbsent(
                    original, k -> "Component_" + counter.getAndIncrement());
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            occurrenceCount++;
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public SanitizationTransformation report() {
        return new SanitizationTransformation(
                SanitizationRuleType.IDENTIFIER_REPLACEMENT,
                occurrenceCount,
                "Identifiants metier remplaces par Component_N");
    }

    @Override
    public SanitizationRuleType ruleType() {
        return SanitizationRuleType.IDENTIFIER_REPLACEMENT;
    }
}
