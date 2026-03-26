package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de SensitiveMarkerDetector (JAS-018 / AI-1).
 */
class SensitiveMarkerDetectorTest {

    private SensitiveMarkerDetector detector;

    @BeforeEach
    void setUp() {
        detector = new SensitiveMarkerDetector();
    }

    @Test
    void should_detect_url_marker() {
        String source = "String endpoint = \"https://internal.company.com/api/v2\";";

        boolean result = detector.hasSensitiveMarkers(source);

        assertThat(result).isTrue();
    }

    @Test
    void should_detect_email_marker() {
        String source = "String contact = \"admin@company.com\";";

        boolean result = detector.hasSensitiveMarkers(source);

        assertThat(result).isTrue();
    }

    @Test
    void should_detect_long_token_without_camelcase() {
        // 40 chars sans transition camelCase — ressemble a un token/secret base64
        String source = "String token = \"abcdefghijklmnopqrstuvwxyz0123456789abcd\";";

        boolean result = detector.hasSensitiveMarkers(source);

        assertThat(result).isTrue();
    }

    @Test
    void should_detect_sensitive_keyword_password() {
        String source = "private String password = getFromConfig();";

        boolean result = detector.hasSensitiveMarkers(source);

        assertThat(result).isTrue();
    }

    @Test
    void should_detect_sensitive_keyword_secret() {
        String source = "final String secret = vault.read(\"app/secret\");";

        boolean result = detector.hasSensitiveMarkers(source);

        assertThat(result).isTrue();
    }

    @Test
    void should_return_false_for_clean_source() {
        String source = "public class OrderService {\n"
                + "    private final OrderRepository repository;\n"
                + "    public void save(Order order) { repository.save(order); }\n"
                + "}";

        boolean result = detector.hasSensitiveMarkers(source);

        assertThat(result).isFalse();
    }

    @Test
    void should_return_false_for_null_input() {
        assertThat(detector.hasSensitiveMarkers(null)).isFalse();
    }

    @Test
    void should_return_false_for_blank_input() {
        assertThat(detector.hasSensitiveMarkers("   ")).isFalse();
    }

    @Test
    void should_allow_long_camelcase_identifier() {
        // Un identifiant Java camelCase de 31+ chars doit etre accepte
        String source = "DonneesEntretienMedicalServiceImpl service = new DonneesEntretienMedicalServiceImpl();";

        boolean result = detector.hasSensitiveMarkers(source);

        assertThat(result).isFalse();
    }
}
