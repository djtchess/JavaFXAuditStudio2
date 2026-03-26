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
 * (Service, Manager, Controller, Repository, Gateway, Handler, Processor, Calculator, Engine).
 * Les identifiants standards Java (List, String, Object…) ne sont pas affectes.
 *
 * <p>Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration}.
 */
public class IdentifierSanitizer implements Sanitizer {

    private static final Pattern BUSINESS_SUFFIX_PATTERN = Pattern.compile(
            "\\b([A-Z][a-z]+(?:[A-Z][a-z]+)*)(?:Service|Manager|Controller|Repository|"
            + "Gateway|Handler|Processor|Calculator|Engine)\\b");

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
