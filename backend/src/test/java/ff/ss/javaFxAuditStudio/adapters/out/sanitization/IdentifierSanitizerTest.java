package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de IdentifierSanitizer (JAS-018).
 */
class IdentifierSanitizerTest {

    private IdentifierSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new IdentifierSanitizer();
    }

    @Test
    void should_replace_service_class_name() {
        String source = "OrderService orderService = new OrderService();";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("OrderService");
        assertThat(result).contains("Component_");
        assertThat(sanitizer.report().occurrenceCount()).isGreaterThan(0);
    }

    @Test
    void should_replace_manager_identifier() {
        String source = "UserManager userManager = context.getBean(UserManager.class);";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("UserManager");
        assertThat(result).contains("Component_");
    }

    @Test
    void should_preserve_standard_java_identifiers() {
        // List, String, Object, Integer, Map, etc. ne doivent pas etre modifies
        String source = "List<String> items = new ArrayList<>(); Object obj = new Object();";

        String result = sanitizer.apply(source);

        assertThat(result).contains("List");
        assertThat(result).contains("String");
        assertThat(result).contains("Object");
        assertThat(sanitizer.report().occurrenceCount()).isZero();
    }

    @Test
    void should_replace_multiple_distinct_identifiers_with_distinct_components() {
        String source = "PaymentGateway pg = new PaymentGateway(); InvoiceHandler ih = new InvoiceHandler();";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("PaymentGateway");
        assertThat(result).doesNotContain("InvoiceHandler");
        // Deux noms distincts → deux Component_N distincts
        assertThat(result).contains("Component_1");
        assertThat(result).contains("Component_2");
    }

    @Test
    void should_replace_controller_identifier() {
        String source = "class CustomerController extends BaseController {}";

        String result = sanitizer.apply(source);

        assertThat(result).doesNotContain("CustomerController");
        assertThat(result).contains("Component_");
    }
}
