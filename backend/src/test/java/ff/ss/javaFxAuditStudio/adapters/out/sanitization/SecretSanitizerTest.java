package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de SecretSanitizer (JAS-018).
 */
class SecretSanitizerTest {

    private SecretSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new SecretSanitizer();
    }

    @Test
    void should_replace_password_in_string() {
        // Le pattern NAMED_SECRET_PATTERN capture les variables nommees 'password'
        String input = "String password = \"mySecretPass123!\";";

        String result = sanitizer.apply(input);

        assertThat(result).doesNotContain("mySecretPass123!");
        assertThat(result).contains("***");
    }

    @Test
    void should_replace_internal_url() {
        String source = "String endpoint = \"https://internal.company.com/api/v2/users\";";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("internal.company.com");
        assertThat(result).contains("INTERNAL_URL");
        assertThat(sanitizer.report().occurrenceCount()).isGreaterThan(0);
    }

    @Test
    void should_replace_long_token() {
        // Token alphanum de 32+ chars isole dans une variable non nommee comme secret
        // pour eviter que NAMED_SECRET_PATTERN ne capture la valeur en premier.
        String source = "String headerValue = \"AbCdEfGhIjKlMnOpQrStUvWxYzAbCdEf\";";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("AbCdEfGhIjKlMnOpQrStUvWxYzAbCdEf");
        assertThat(result).contains("REDACTED");
    }

    @Test
    void should_report_zero_occurrences_for_clean_source() {
        String source = "int x = 42; String name = \"test\";";

        sanitizer.apply(source);

        assertThat(sanitizer.report().occurrenceCount()).isZero();
    }

    @Test
    void should_replace_apikey_variable() {
        String source = "apiKey = \"ABCDEFGHIJ\";";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("ABCDEFGHIJ");
    }
}
