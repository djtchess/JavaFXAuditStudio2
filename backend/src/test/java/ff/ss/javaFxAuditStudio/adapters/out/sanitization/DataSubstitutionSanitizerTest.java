package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de DataSubstitutionSanitizer (AI-4-T2, JAS-018).
 *
 * <p>Semantique verifiee :
 * - Emails → user@example.com
 * - Nombres >= 7 chiffres consecutifs → 0000000
 * - Chaines entre guillemets > 20 chars non-Java/Spring → "[data]"
 */
class DataSubstitutionSanitizerTest {

    private DataSubstitutionSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new DataSubstitutionSanitizer();
    }

    @Test
    void should_replace_email_with_placeholder() {
        String source = "String contact = \"user@company.com\";";

        String result = sanitizer.apply(source);

        assertThat(result).contains("user@example.com");
        assertThat(result).doesNotContain("user@company.com");
    }

    @Test
    void should_replace_long_number_with_placeholder() {
        // 7+ chiffres consecutifs delimites par des word-boundaries → 0000000
        // Note : LONG_NUMBER_PATTERN utilise \b donc le nombre doit etre isole (pas suivi de lettre)
        String source = "long card = 9876543210;";

        String result = sanitizer.apply(source);

        assertThat(result).contains("0000000");
        assertThat(result).doesNotContain("9876543210");
    }

    @Test
    void should_replace_long_string_literal_with_placeholder() {
        // Chaine de plus de 20 chars non-Java/Spring entre guillemets
        String source = "String key = \"my-super-secret-api-key-12345\";";

        String result = sanitizer.apply(source);

        assertThat(result).contains("\"[data]\"");
        assertThat(result).doesNotContain("my-super-secret-api-key-12345");
    }

    @Test
    void should_preserve_source_without_substitutable_data() {
        String source = "int count = 42; boolean active = true;";

        String result = sanitizer.apply(source);

        assertThat(result).isEqualTo(source);
    }

    @Test
    void should_replace_multiple_substitutions_in_same_source() {
        String source = "String a = \"admin@corp.org\"; long b = 12345678L; String c = \"admin@intern.net\";";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("admin@corp.org");
        assertThat(result).doesNotContain("admin@intern.net");
        assertThat(result).doesNotContain("12345678");
        assertThat(result.indexOf("user@example.com")).isNotEqualTo(result.lastIndexOf("user@example.com"));
    }

    @Test
    void should_report_rule_type_as_data_substitution() {
        sanitizer.apply("String x = \"dummy@test.com\";");

        assertThat(sanitizer.report().ruleType()).isEqualTo(SanitizationRuleType.DATA_SUBSTITUTION);
    }

    @Test
    void should_preserve_java_spring_path_string() {
        // Chaine de plus de 20 chars mais correspondant au pattern Java/Spring
        // JAVA_PATTERN = ^[a-z./\-_{}$#@:\[\]()]+$ (pas de chiffres, pas de majuscules)
        // Ex: "/api/users/{id}/orders/items" -> que des chars minuscules et separateurs
        String source = "String path = \"/api/users/{id}/orders/items/details\";";

        String result = sanitizer.apply(source);

        // Le chemin respecte JAVA_PATTERN => pas de substitution
        assertThat(result).contains("/api/users/{id}/orders/items/details");
    }

    @Test
    void should_count_occurrences_in_report() {
        // Un email + un long numero isole = 2 occurrences
        // Note : le nombre ne doit pas etre suivi d'une lettre (contrainte \b du pattern)
        String source = "String e = \"dev@test.io\"; long n = 99999999;";

        sanitizer.apply(source);

        assertThat(sanitizer.report().occurrenceCount()).isEqualTo(2);
    }
}
