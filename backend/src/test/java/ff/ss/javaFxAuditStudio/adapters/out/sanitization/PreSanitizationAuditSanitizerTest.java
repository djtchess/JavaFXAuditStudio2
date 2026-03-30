package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationTransformation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de PreSanitizationAuditSanitizer (Action 6 / JAS-018).
 *
 * <p>Verifie que :
 * <ul>
 *   <li>Le source retourne est strictement identique au source en entree (pas de modification).</li>
 *   <li>Les elements sensibles sont correctement comptabilises.</li>
 *   <li>Le rapport porte le type {@link SanitizationRuleType#PRE_SANITIZATION_AUDIT}.</li>
 *   <li>Un source sans element sensible produit un compteur a zero.</li>
 * </ul>
 */
class PreSanitizationAuditSanitizerTest {

    private PreSanitizationAuditSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new PreSanitizationAuditSanitizer();
    }

    // --- Invariant fondamental : aucune modification du source ---

    @Test
    void apply_should_return_source_unchanged_when_no_sensitive_elements() {
        String source = "class Foo { void bar() { int x = 1; } }";

        String result = sanitizer.apply(source);

        assertThat(result).isEqualTo(source);
    }

    @Test
    void apply_should_return_source_unchanged_when_business_class_present() {
        String source = "class OrderService { void process() {} }";

        String result = sanitizer.apply(source);

        assertThat(result).isEqualTo(source);
    }

    @Test
    void apply_should_return_source_unchanged_when_secret_fields_present() {
        String source = "private String password = \"secret123\";";

        String result = sanitizer.apply(source);

        assertThat(result).isEqualTo(source);
    }

    @Test
    void apply_should_return_source_unchanged_when_url_present() {
        String source = "String url = \"https://api.example.com/v1\";";

        String result = sanitizer.apply(source);

        assertThat(result).isEqualTo(source);
    }

    @Test
    void apply_should_return_source_unchanged_when_email_present() {
        String source = "String contact = \"alice@example.com\";";

        String result = sanitizer.apply(source);

        assertThat(result).isEqualTo(source);
    }

    @Test
    void apply_should_return_source_unchanged_when_jpa_annotations_present() {
        String source = "@Entity\npublic class Invoice {\n  @Id\n  @Column(name=\"id\")\n  private Long id;\n}";

        String result = sanitizer.apply(source);

        assertThat(result).isEqualTo(source);
    }

    @Test
    void apply_should_return_source_unchanged_when_comments_present() {
        String source = "// commentaire sensible\n/* bloc */\nvoid foo() {}";

        String result = sanitizer.apply(source);

        assertThat(result).isEqualTo(source);
    }

    // --- Comptage des elements detectes ---

    @Test
    void report_ruleType_should_be_PRE_SANITIZATION_AUDIT() {
        sanitizer.apply("class Foo {}");

        SanitizationTransformation report = sanitizer.report();

        assertThat(report.ruleType()).isEqualTo(SanitizationRuleType.PRE_SANITIZATION_AUDIT);
    }

    @Test
    void report_occurrenceCount_should_be_zero_when_no_sensitive_pattern() {
        sanitizer.apply("class Foo { void bar() { int x = 1; } }");

        assertThat(sanitizer.report().occurrenceCount()).isZero();
    }

    @Test
    void report_occurrenceCount_should_count_business_class_names() {
        // OrderService est un identifiant metier : doit etre comptabilise
        String source = "class OrderService { void run() {} }";

        sanitizer.apply(source);

        assertThat(sanitizer.report().occurrenceCount()).isGreaterThan(0);
    }

    @Test
    void report_occurrenceCount_should_count_url() {
        String source = "String u = \"https://pay.example.com/api\";";

        sanitizer.apply(source);

        assertThat(sanitizer.report().occurrenceCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void report_occurrenceCount_should_count_email() {
        String source = "String mail = \"admin@corp.io\";";

        sanitizer.apply(source);

        assertThat(sanitizer.report().occurrenceCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void report_occurrenceCount_should_count_jpa_annotations() {
        String source = "@Entity\npublic class Invoice {\n  @Id\n  @Column(name=\"ref\")\n  private Long id;\n}";

        sanitizer.apply(source);

        // @Entity, @Id, @Column = 3 annotations JPA au minimum
        assertThat(sanitizer.report().occurrenceCount()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void report_occurrenceCount_should_accumulate_all_categories() {
        // Source avec plusieurs categories presentes simultanement
        String source = """
                @Entity
                class InvoiceService {
                    // TODO: remove credentials
                    @Value("${api.key}")
                    private String apiKey;
                    @Column(name = "email")
                    private String contact = "admin@corp.io";
                    String url = "https://api.example.com";
                }
                """;

        sanitizer.apply(source);

        // Doit detecter au minimum : classe metier, @Value secret, URL, email, commentaire, @Entity, @Column
        assertThat(sanitizer.report().occurrenceCount()).isGreaterThan(5);
    }

    @Test
    void report_occurrenceCount_should_reset_between_calls() {
        // Premier appel avec source riche
        String richSource = "class OrderService { String url = \"https://api.example.com\"; }";
        sanitizer.apply(richSource);
        int firstCount = sanitizer.report().occurrenceCount();
        assertThat(firstCount).isGreaterThan(0);

        // Deuxieme appel avec source vide de tout element sensible
        sanitizer.apply("class Foo { void bar() {} }");
        int secondCount = sanitizer.report().occurrenceCount();

        assertThat(secondCount).isLessThan(firstCount);
    }

    @Test
    void ruleType_should_return_PRE_SANITIZATION_AUDIT() {
        assertThat(sanitizer.ruleType()).isEqualTo(SanitizationRuleType.PRE_SANITIZATION_AUDIT);
    }

    @Test
    void report_description_should_mention_detected_count() {
        sanitizer.apply("class Foo {}");

        assertThat(sanitizer.report().description()).isNotBlank();
        assertThat(sanitizer.report().description()).contains("Audit");
    }
}
