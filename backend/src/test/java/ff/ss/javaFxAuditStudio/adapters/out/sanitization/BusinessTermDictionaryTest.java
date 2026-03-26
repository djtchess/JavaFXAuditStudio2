package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifie que {@link BusinessTermDictionary} est la source unique de verite
 * pour les suffixes metier et que les deux sanitizers produisent un comportement
 * coherent sur les memes entrees (QW-2 — CA-1, CA-2, CA-3, CA-4).
 */
class BusinessTermDictionaryTest {

    // -------------------------------------------------------------------------
    // CA-1 : unicite de la source de verite
    // -------------------------------------------------------------------------

    @Test
    void business_suffixes_list_is_not_empty() {
        assertThat(BusinessTermDictionary.BUSINESS_SUFFIXES).isNotEmpty();
    }

    @Test
    void business_suffixes_alternation_contains_all_list_entries() {
        String alternation = BusinessTermDictionary.BUSINESS_SUFFIXES_ALTERNATION;
        for (String suffix : BusinessTermDictionary.BUSINESS_SUFFIXES) {
            assertThat(alternation).contains(suffix);
        }
    }

    @Test
    void class_name_pattern_matches_service_suffix() {
        boolean matches = BusinessTermDictionary.CLASS_NAME_PATTERN
                .matcher("OrderService").matches();
        assertThat(matches).isTrue();
    }

    @Test
    void class_name_pattern_does_not_match_plain_dto() {
        boolean matches = BusinessTermDictionary.CLASS_NAME_PATTERN
                .matcher("OrderDto").matches();
        assertThat(matches).isFalse();
    }

    @Test
    void business_identifier_pattern_matches_camelcase_with_suffix() {
        boolean found = BusinessTermDictionary.BUSINESS_IDENTIFIER_PATTERN
                .matcher("PaymentGateway gateway = new PaymentGateway();").find();
        assertThat(found).isTrue();
    }

    @Test
    void class_declaration_pattern_matches_class_keyword_followed_by_business_name() {
        boolean found = BusinessTermDictionary.CLASS_DECLARATION_PATTERN
                .matcher("public class InvoiceRepository {").find();
        assertThat(found).isTrue();
    }

    // -------------------------------------------------------------------------
    // CA-2 + CA-3 : les deux sanitizers utilisent la meme source
    // -------------------------------------------------------------------------

    @Test
    void identifier_sanitizer_uses_dictionary_suffix_for_service() {
        IdentifierSanitizer sanitizer = new IdentifierSanitizer();
        String result = sanitizer.apply("OrderService orderService = new OrderService();");
        assertThat(result).doesNotContain("OrderService");
        assertThat(result).contains("Component_");
    }

    @Test
    void openrewrite_sanitizer_uses_dictionary_suffix_for_service() {
        OpenRewriteIdentifierSanitizer sanitizer = new OpenRewriteIdentifierSanitizer();
        String result = sanitizer.apply("public class OrderService { void execute() {} }");
        assertThat(result).doesNotContain("OrderService");
    }

    @Test
    void both_sanitizers_recognize_all_dictionary_suffixes() {
        IdentifierSanitizer idSanitizer = new IdentifierSanitizer();
        OpenRewriteIdentifierSanitizer orSanitizer = new OpenRewriteIdentifierSanitizer();

        for (String suffix : BusinessTermDictionary.BUSINESS_SUFFIXES) {
            String className = "Acme" + suffix;
            String idSource = className + " x = new " + className + "();";
            String orSource = "class " + className + " { }";

            String idResult = idSanitizer.apply(idSource);
            String orResult = orSanitizer.apply(orSource);

            assertThat(idResult)
                    .as("IdentifierSanitizer doit neutraliser le suffixe : " + suffix)
                    .doesNotContain(className);
            assertThat(orResult)
                    .as("OpenRewriteIdentifierSanitizer doit neutraliser le suffixe : " + suffix)
                    .doesNotContain(className);
        }
    }

    // -------------------------------------------------------------------------
    // CA-4 : test nominal de coherence comportementale entre les deux sanitizers
    // -------------------------------------------------------------------------

    @Test
    void both_sanitizers_neutralize_business_name_without_exposing_it() {
        IdentifierSanitizer idSanitizer = new IdentifierSanitizer();
        OpenRewriteIdentifierSanitizer orSanitizer = new OpenRewriteIdentifierSanitizer();

        String businessName = "CustomerManager";

        String idResult = idSanitizer.apply(businessName + " mgr = new " + businessName + "();");
        String orResult = orSanitizer.apply("class " + businessName + " { }");

        assertThat(idResult)
                .as("IdentifierSanitizer : le nom metier ne doit plus apparaitre")
                .doesNotContain(businessName);
        assertThat(orResult)
                .as("OpenRewriteIdentifierSanitizer : le nom metier ne doit plus apparaitre")
                .doesNotContain(businessName);

        assertThat(idSanitizer.report().occurrenceCount())
                .as("IdentifierSanitizer doit signaler au moins un remplacement")
                .isGreaterThan(0);
        assertThat(orSanitizer.report().occurrenceCount())
                .as("OpenRewriteIdentifierSanitizer doit signaler au moins un remplacement")
                .isGreaterThan(0);
    }
}
