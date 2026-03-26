package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationTransformation;

/**
 * Remplace les donnees reelles dans les chaines litterales (JAS-018).
 *
 * <p>Regles appliquees dans l'ordre :
 * <ol>
 *   <li>Emails → {@code user@example.com}</li>
 *   <li>Numeros de plus de 6 chiffres consecutifs → {@code 0000000}</li>
 *   <li>Chaines entre guillemets de plus de 20 chars non-Java/Spring → {@code "[data]"}</li>
 * </ol>
 *
 * <p>Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration}.
 */
public class DataSubstitutionSanitizer implements Sanitizer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    private static final Pattern LONG_NUMBER_PATTERN = Pattern.compile(
            "\\b\\d{7,}\\b");

    // Chaines entre guillemets > 20 chars qui ne ressemblent pas a des patterns Java/Spring
    private static final Pattern LONG_STRING_PATTERN = Pattern.compile(
            "\"([^\"\\\\]{21,})\"");

    // Patterns Java/Spring typiques a conserver (chemins, annotations, mots-cles)
    private static final Pattern JAVA_PATTERN = Pattern.compile(
            "^[a-z./\\-_{}$#@:\\[\\]()]+$");

    private int occurrenceCount;

    @Override
    public String apply(final String source) {
        occurrenceCount = 0;
        String result = source;
        result = replaceAll(result, EMAIL_PATTERN, m -> "user@example.com");
        result = replaceAll(result, LONG_NUMBER_PATTERN, m -> "0000000");
        result = replaceLongStrings(result);
        return result;
    }

    private String replaceAll(
            final String source,
            final Pattern pattern,
            final java.util.function.Function<Matcher, String> replacer) {
        Matcher matcher = pattern.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb,
                    Matcher.quoteReplacement(replacer.apply(matcher)));
            occurrenceCount++;
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String replaceLongStrings(final String source) {
        Matcher matcher = LONG_STRING_PATTERN.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String content = matcher.group(1);
            if (!JAVA_PATTERN.matcher(content).matches()) {
                matcher.appendReplacement(sb, "\"[data]\"");
                occurrenceCount++;
            } else {
                matcher.appendReplacement(sb,
                        Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public SanitizationTransformation report() {
        return new SanitizationTransformation(
                SanitizationRuleType.DATA_SUBSTITUTION,
                occurrenceCount,
                "Emails, numeros et chaines de donnees remplaces par des valeurs fictives");
    }

    @Override
    public SanitizationRuleType ruleType() {
        return SanitizationRuleType.DATA_SUBSTITUTION;
    }
}
