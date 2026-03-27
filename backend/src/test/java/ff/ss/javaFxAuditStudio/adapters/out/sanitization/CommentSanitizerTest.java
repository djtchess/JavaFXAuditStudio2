package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de CommentSanitizer (AI-4-T1, JAS-018).
 *
 * <p>Semantique verifiee :
 * - Blocs /* ... * / et Javadoc → remplaces par /* [removed] * /
 * - Commentaires // contenant nom propre / chiffre / acronyme → supprimes
 * - Commentaires // purement techniques → conserves
 */
class CommentSanitizerTest {

    private CommentSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new CommentSanitizer();
    }

    @Test
    void should_neutralize_block_comment() {
        String source = "int x = 1; /* bloc sensible */ int y = 2;";

        String result = sanitizer.apply(source);

        assertThat(result).contains("/* [removed] */");
        assertThat(result).doesNotContain("bloc sensible");
    }

    @Test
    void should_remove_single_line_comment_with_proper_noun() {
        // "John" = nom propre (majuscule + >= 3 minuscules)
        String source = "void init() {\n// John wrote this method\nreturn;\n}";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("John wrote this method");
    }

    @Test
    void should_neutralize_javadoc_comment() {
        String source = "/** Author: Alice Smith — contact intern */\npublic void run() {}";

        String result = sanitizer.apply(source);

        assertThat(result).contains("/* [removed] */");
        assertThat(result).doesNotContain("Alice Smith");
    }

    @Test
    void should_preserve_source_without_comments() {
        String source = "public class Foo { void bar() { int x = 42; } }";

        String result = sanitizer.apply(source);

        assertThat(result).isEqualTo(source);
    }

    @Test
    void should_remove_single_line_comment_containing_url() {
        // URL contient "http" qui satisfait la regex SENSITIVE_SINGLE_LINE
        // via au moins un nom propre ou chiffres -> "http" ne suffit pas seul
        // mais une URL du style http://example.com contient "example" (majuscule + min)
        // En fait verifions : pattern = //.*(?:[A-Z][a-z]{2,}|\d{3,}|[A-Z]{4,})
        // "http://user.internal.com/api/1234" contient \d{3,} -> detecte
        String source = "void load() {\n// see http://192.168.1.1/api/1234\nreturn;\n}";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("192.168.1.1");
        assertThat(result).doesNotContain("http://");
    }

    @Test
    void should_report_rule_type_as_comment_removal() {
        sanitizer.apply("// dummy");

        assertThat(sanitizer.report().ruleType()).isEqualTo(SanitizationRuleType.COMMENT_REMOVAL);
    }

    @Test
    void should_preserve_technical_single_line_comment() {
        // Commentaire purement technique : pas de nom propre, pas de chiffre >= 3 digits,
        // pas d'acronyme >= 4 majuscules
        String source = "void run() {\n// ok\nreturn;\n}";

        String result = sanitizer.apply(source);

        assertThat(result).contains("// ok");
    }

    @Test
    void should_count_occurrences_in_report() {
        // Deux blocs : chacun incrementer le compteur
        String source = "/* bloc1 */ int a = 1; /* bloc2 */ int b = 2;";

        sanitizer.apply(source);

        assertThat(sanitizer.report().occurrenceCount()).isEqualTo(2);
    }
}
