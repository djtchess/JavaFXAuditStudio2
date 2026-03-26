package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationTransformation;

/**
 * Supprime ou neutralise les commentaires sensibles (JAS-018).
 *
 * <p>Regles appliquees :
 * <ul>
 *   <li>Commentaires multi-lignes {@code /* ... * /} et Javadoc → {@code /* [removed] * /}</li>
 *   <li>Commentaires single-line {@code //} contenant un nom propre, un numero
 *       ou un acronyme metier (> 3 chars majuscules) → ligne supprimee</li>
 *   <li>Commentaires single-line purement techniques → conserves</li>
 * </ul>
 *
 * <p>Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration}.
 */
public class CommentSanitizer implements Sanitizer {

    private static final Pattern BLOCK_COMMENT_PATTERN = Pattern.compile(
            "/\\*.*?\\*/", Pattern.DOTALL);

    // Detecte noms propres (majuscule + minuscules), numeros, acronymes metier > 3 chars
    private static final Pattern SENSITIVE_SINGLE_LINE = Pattern.compile(
            "//.*(?:[A-Z][a-z]{2,}|\\d{3,}|[A-Z]{4,}).*");

    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile(
            "//[^\n]*");

    private int occurrenceCount;

    @Override
    public String apply(final String source) {
        occurrenceCount = 0;
        String result = replaceBlockComments(source);
        result = filterSingleLineComments(result);
        return result;
    }

    private String replaceBlockComments(final String source) {
        Matcher matcher = BLOCK_COMMENT_PATTERN.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "/* [removed] */");
            occurrenceCount++;
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String filterSingleLineComments(final String source) {
        Matcher matcher = SINGLE_LINE_COMMENT.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String comment = matcher.group();
            if (SENSITIVE_SINGLE_LINE.matcher(comment).matches()) {
                matcher.appendReplacement(sb, "");
                occurrenceCount++;
            } else {
                matcher.appendReplacement(sb,
                        Matcher.quoteReplacement(comment));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public SanitizationTransformation report() {
        return new SanitizationTransformation(
                SanitizationRuleType.COMMENT_REMOVAL,
                occurrenceCount,
                "Commentaires sensibles supprimes ou neutralises");
    }

    @Override
    public SanitizationRuleType ruleType() {
        return SanitizationRuleType.COMMENT_REMOVAL;
    }
}
