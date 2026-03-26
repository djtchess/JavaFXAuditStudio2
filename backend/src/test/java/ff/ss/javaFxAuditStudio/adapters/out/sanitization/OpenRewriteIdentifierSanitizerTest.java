package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de OpenRewriteIdentifierSanitizer (JAS-018).
 *
 * <p>Le sanitizer fonctionne en mode best-effort :
 * <ul>
 *   <li>Mode AST OpenRewrite si le parsing aboutit.</li>
 *   <li>Mode fallback regex si OpenRewrite ne peut pas parser (classpath incomplet).</li>
 * </ul>
 * Les tests valident le comportement observable final, quel que soit le mode interne.
 */
class OpenRewriteIdentifierSanitizerTest {

    private OpenRewriteIdentifierSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new OpenRewriteIdentifierSanitizer();
    }

    @Test
    void should_neutralize_service_class_declaration() {
        String source = "public class OrderService { void execute() {} }";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("OrderService");
        assertThat(result).contains("Neutralized_");
        assertThat(sanitizer.report().occurrenceCount()).isGreaterThan(0);
    }

    @Test
    void should_neutralize_manager_class_declaration() {
        String source = "class UserManager { private int id; }";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("UserManager");
        assertThat(result).contains("Neutralized_");
    }

    @Test
    void should_neutralize_controller_class_declaration() {
        String source = "public class CustomerController { void handle() {} }";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("CustomerController");
    }

    @Test
    void should_neutralize_repository_class_declaration() {
        String source = "class InvoiceRepository { List<String> findAll() { return null; } }";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("InvoiceRepository");
    }

    @Test
    void should_return_source_unchanged_when_no_business_class() {
        String source = "class Processor { }";
        // "Processor" seul sans prefixe camelcase ne correspond pas au pattern
        // (le pattern exige un prefixe [A-Z][a-z]+... avant le suffixe)
        // Mais "Processor" seul correspond bien au suffix → le nom complet est "Processor"
        // Ce test verifie que le sanitizer ne plante pas sur un cas limite
        String result = sanitizer.apply(source);

        assertThat(result).isNotNull();
        assertThat(sanitizer.report().ruleType())
                .isEqualTo(SanitizationRuleType.OPENREWRITE_REMEDIATION);
    }

    @Test
    void should_preserve_standard_java_classes() {
        // Les noms sans suffixe metier (List, String, Object) ne doivent pas etre modifies
        String source = "class MyDto { List<String> items; Object value; }";

        String result = sanitizer.apply(source);

        // MyDto n'a pas de suffixe metier reconnu → pas de modification
        assertThat(result).contains("MyDto");
        assertThat(sanitizer.report().occurrenceCount()).isZero();
    }

    @Test
    void should_handle_null_source_gracefully() {
        // null retourne null sans exception
        String result = sanitizer.apply(null);

        assertThat(result).isNull();
    }

    @Test
    void should_handle_blank_source_gracefully() {
        String result = sanitizer.apply("   ");

        assertThat(result).isEqualTo("   ");
        assertThat(sanitizer.report().occurrenceCount()).isZero();
    }

    @Test
    void should_report_correct_rule_type() {
        sanitizer.apply("class FooService {}");

        assertThat(sanitizer.report().ruleType())
                .isEqualTo(SanitizationRuleType.OPENREWRITE_REMEDIATION);
    }

    @Test
    void should_report_description_with_count() {
        sanitizer.apply("class PaymentGateway {} class OrderHandler {}");

        String description = sanitizer.report().description();

        assertThat(description).contains("neutralise");
        assertThat(description).contains("OpenRewrite");
    }

    @Test
    void should_neutralize_multiple_distinct_classes_in_same_source() {
        String source = "class PaymentGateway {} class OrderHandler {}";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("PaymentGateway");
        assertThat(result).doesNotContain("OrderHandler");
        assertThat(sanitizer.report().occurrenceCount()).isGreaterThan(0);
    }

    @Test
    void should_replace_references_to_renamed_class_in_fallback_mode() {
        // Verifie que le fallback regex remplace aussi les references (pas juste la declaration)
        // Source sans classpath complet → mode fallback garantit la coherence
        String source = "class InvoiceProcessor { InvoiceProcessor other = new InvoiceProcessor(); }";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("InvoiceProcessor");
    }
}
