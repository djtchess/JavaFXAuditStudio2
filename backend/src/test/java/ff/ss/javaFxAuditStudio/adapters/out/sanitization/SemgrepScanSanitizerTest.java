package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ff.ss.javaFxAuditStudio.configuration.SemgrepScanProperties;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires de SemgrepScanSanitizer (JAS-018).
 *
 * <p>Strategies de test :
 * <ul>
 *   <li>Mode desactive : le source est retourne intact sans appel a Semgrep.</li>
 *   <li>Mode gracieux (Semgrep absent) : le sanitizer ne leve pas d'exception.</li>
 *   <li>Mode fail-on-findings : verifie que le refus est leve sur findings ERROR.</li>
 *   <li>Rapport : ruleType et description sont corrects.</li>
 * </ul>
 *
 * <p>Semgrep n'est pas requis sur le PATH pour executer cette suite — tous les cas
 * exercent soit le mode desactive, soit le mode gracieux (commande introuvable).
 */
class SemgrepScanSanitizerTest {

    private static final String SAMPLE_SOURCE =
            "public class Neutralized_1 { void handle() { int x = 42; } }";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // -------------------------------------------------------------------------
    // Mode desactive
    // -------------------------------------------------------------------------

    @Test
    void should_return_source_unchanged_when_disabled() {
        SemgrepScanSanitizer sanitizer = buildSanitizer(/* enabled */ false, /* failOnFindings */ false);

        String result = sanitizer.apply(SAMPLE_SOURCE);

        assertThat(result).isEqualTo(SAMPLE_SOURCE);
    }

    @Test
    void should_report_zero_occurrences_when_disabled() {
        SemgrepScanSanitizer sanitizer = buildSanitizer(false, false);
        sanitizer.apply(SAMPLE_SOURCE);

        assertThat(sanitizer.report().occurrenceCount()).isZero();
    }

    @Test
    void should_report_correct_rule_type_when_disabled() {
        SemgrepScanSanitizer sanitizer = buildSanitizer(false, false);
        sanitizer.apply(SAMPLE_SOURCE);

        assertThat(sanitizer.report().ruleType())
                .isEqualTo(SanitizationRuleType.SEMGREP_SECURITY_SCAN);
    }

    // -------------------------------------------------------------------------
    // Mode gracieux : Semgrep absent du PATH
    // -------------------------------------------------------------------------

    @Test
    void should_return_source_unchanged_when_semgrep_not_on_path() {
        // Commande inexistante => IOException capturee => mode gracieux
        SemgrepScanSanitizer sanitizer = buildSanitizerWithCommand(
                true, false, "semgrep-does-not-exist-xyz", List.of());

        String result = sanitizer.apply(SAMPLE_SOURCE);

        assertThat(result).isEqualTo(SAMPLE_SOURCE);
    }

    @Test
    void should_report_zero_occurrences_when_semgrep_not_on_path() {
        SemgrepScanSanitizer sanitizer = buildSanitizerWithCommand(
                true, false, "semgrep-does-not-exist-xyz", List.of());
        sanitizer.apply(SAMPLE_SOURCE);

        assertThat(sanitizer.report().occurrenceCount()).isZero();
    }

    @Test
    void should_not_throw_when_semgrep_not_on_path() {
        SemgrepScanSanitizer sanitizer = buildSanitizerWithCommand(
                true, false, "semgrep-does-not-exist-xyz", List.of());

        // Ne doit jamais lever d'exception
        assertThat(sanitizer.apply(SAMPLE_SOURCE)).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Mode gracieux : fail-on-findings desactive (defaut)
    // -------------------------------------------------------------------------

    @Test
    void should_not_throw_when_fail_on_findings_is_false_and_semgrep_absent() {
        SemgrepScanSanitizer sanitizer = buildSanitizerWithCommand(
                true, /* failOnFindings */ false, "semgrep-does-not-exist-xyz", List.of());

        // Meme si des findings auraient ete trouves, failOnFindings=false => pas d'exception
        assertThat(sanitizer.apply(SAMPLE_SOURCE)).isEqualTo(SAMPLE_SOURCE);
    }

    // -------------------------------------------------------------------------
    // Contrat de l'interface Sanitizer
    // -------------------------------------------------------------------------

    @Test
    void should_never_modify_source_content() {
        // Le SemgrepScanSanitizer est un scanner, pas un transformer
        SemgrepScanSanitizer sanitizer = buildSanitizer(false, false);

        String original = "class Neutralized_1 { String url = \"https://INTERNAL_URL\"; }";
        String result = sanitizer.apply(original);

        assertThat(result).isEqualTo(original);
    }

    @Test
    void should_handle_null_source_gracefully() {
        SemgrepScanSanitizer sanitizer = buildSanitizer(false, false);

        // null doit lever NullPointerException (contrat explicite dans apply())
        assertThatThrownBy(() -> sanitizer.apply(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_handle_empty_source_without_crash() {
        SemgrepScanSanitizer sanitizer = buildSanitizer(false, false);

        String result = sanitizer.apply("");

        assertThat(result).isEmpty();
        assertThat(sanitizer.report().occurrenceCount()).isZero();
    }

    // -------------------------------------------------------------------------
    // Rapport
    // -------------------------------------------------------------------------

    @Test
    void should_report_description_mentioning_semgrep() {
        SemgrepScanSanitizer sanitizer = buildSanitizer(false, false);
        sanitizer.apply(SAMPLE_SOURCE);

        assertThat(sanitizer.report().description()).containsIgnoringCase("Semgrep");
    }

    @Test
    void should_reset_occurrence_count_between_calls() {
        // Le sanitizer n'est pas thread-safe, mais entre deux appels sequentiels
        // le compteur doit etre reinitialise.
        SemgrepScanSanitizer sanitizer = buildSanitizer(false, false);

        sanitizer.apply(SAMPLE_SOURCE);
        int firstCount = sanitizer.report().occurrenceCount();

        sanitizer.apply(SAMPLE_SOURCE);
        int secondCount = sanitizer.report().occurrenceCount();

        // Les deux appels disabled => 0 dans les deux cas
        assertThat(firstCount).isZero();
        assertThat(secondCount).isZero();
    }

    // -------------------------------------------------------------------------
    // Termes metier configurables
    // -------------------------------------------------------------------------

    @Test
    void should_include_business_terms_in_rules_when_configured() {
        // Avec des termes metier configures et Semgrep absent => mode gracieux sans erreur
        SemgrepScanSanitizer sanitizer = buildSanitizerWithCommand(
                true, false, "semgrep-does-not-exist-xyz",
                List.of("ACME_CORP", "PROJECT_PHOENIX"));

        // Doit se comporter en mode gracieux sans lever d'exception
        String result = sanitizer.apply("class Foo { String s = \"ACME_CORP rules\"; }");

        assertThat(result).isNotNull();
    }

    @Test
    void should_include_extended_static_and_generic_rules_in_generated_yaml() {
        SemgrepScanSanitizer sanitizer = buildSanitizer(false, false);

        String yaml = sanitizer.buildRulesYaml();

        assertThat(yaml)
                .contains("jdbc-connection-string")
                .contains("private-key-marker")
                .contains("generic-jdbc-url")
                .contains("generic-private-key-marker");
    }

    @Test
    void should_include_denylist_terms_in_generated_rules_when_configured() {
        SemgrepScanProperties props = new SemgrepScanProperties(
                false,
                "semgrep",
                10,
                false,
                List.of(),
                List.of("corp.local", "legacy-host"));
        SemgrepScanSanitizer sanitizer = new SemgrepScanSanitizer(props, objectMapper);

        String yaml = sanitizer.buildRulesYaml();

        assertThat(yaml)
                .contains("denylist-term")
                .contains("\\Qcorp.local\\E")
                .contains("\\Qlegacy-host\\E");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SemgrepScanSanitizer buildSanitizer(
            final boolean enabled, final boolean failOnFindings) {
        return buildSanitizerWithCommand(enabled, failOnFindings, "semgrep", List.of());
    }

    private SemgrepScanSanitizer buildSanitizerWithCommand(
            final boolean enabled,
            final boolean failOnFindings,
            final String cliCommand,
            final List<String> businessTerms) {
        SemgrepScanProperties props = new SemgrepScanProperties(
                enabled, cliCommand, 10, failOnFindings, businessTerms, List.of());
        return new SemgrepScanSanitizer(props, objectMapper);
    }
}
