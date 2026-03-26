package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizableFile;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationReport;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationTransformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de MultiFileSanitizationAdapter (QW-5).
 */
@ExtendWith(MockitoExtension.class)
class MultiFileSanitizationAdapterTest {

    @Mock
    private SanitizationPort sanitizationPort;

    @Mock
    private SecretSanitizer secretSanitizer;

    @Mock
    private SemgrepScanSanitizer semgrepScanSanitizer;

    private MultiFileSanitizationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MultiFileSanitizationAdapter(sanitizationPort, secretSanitizer, semgrepScanSanitizer);
    }

    // --- Test liste vide ---

    @Test
    void should_return_empty_list_when_no_files_given() {
        List<SanitizedBundle> result = adapter.sanitizeFiles("bundle-1", List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(sanitizationPort, secretSanitizer, semgrepScanSanitizer);
    }

    // --- Test fichier Java : delegation a SanitizationPort ---

    @Test
    void should_delegate_java_file_to_sanitization_port() {
        SanitizableFile javaFile = new SanitizableFile("MyController.java", "class MyController {}", "java");
        SanitizedBundle expected = buildApprovedBundle("bundle-java", "MyController.java", "class MyController {}");
        when(sanitizationPort.sanitize(eq("bundle-java"), eq("class MyController {}"), eq("MyController.java")))
                .thenReturn(expected);

        List<SanitizedBundle> result = adapter.sanitizeFiles("bundle-java", List.of(javaFile));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(expected);
        verify(sanitizationPort).sanitize("bundle-java", "class MyController {}", "MyController.java");
        verifyNoInteractions(secretSanitizer, semgrepScanSanitizer);
    }

    // --- Test fichier YAML : uniquement SecretSanitizer + SemgrepScanSanitizer ---

    @Test
    void should_apply_secret_and_semgrep_for_yaml_file() {
        SanitizableFile yamlFile = new SanitizableFile(
                "application.yaml", "password: mypassword123", "yaml");

        String afterSecret = "password: ***";
        SanitizationTransformation secretTransfo = new SanitizationTransformation(
                SanitizationRuleType.SECRET_REMOVAL, 1, "Secrets remplaces");
        SanitizationTransformation semgrepTransfo = new SanitizationTransformation(
                SanitizationRuleType.SEMGREP_SECURITY_SCAN, 0, "Semgrep : 0 finding(s)");

        when(secretSanitizer.apply("password: mypassword123")).thenReturn(afterSecret);
        when(secretSanitizer.report()).thenReturn(secretTransfo);
        when(semgrepScanSanitizer.applyToFile(
                new SanitizableFile("application.yaml", afterSecret, "yaml")))
                .thenReturn(afterSecret);
        when(semgrepScanSanitizer.report()).thenReturn(semgrepTransfo);

        List<SanitizedBundle> result = adapter.sanitizeFiles("bundle-yaml", List.of(yamlFile));

        assertThat(result).hasSize(1);
        SanitizedBundle bundle = result.get(0);
        assertThat(bundle.bundleId()).isEqualTo("bundle-yaml");
        assertThat(bundle.controllerRef()).isEqualTo("application.yaml");
        assertThat(bundle.sanitizedSource()).isEqualTo(afterSecret);
        assertThat(bundle.report()).isNotNull();
        assertThat(bundle.report().approved()).isTrue();
        assertThat(bundle.report().transformations()).hasSize(2);
        verifyNoInteractions(sanitizationPort);
    }

    // --- Test fichier properties : detection secret via SecretSanitizer ---

    @Test
    void should_sanitize_properties_file_with_secret_detection() {
        SanitizableFile propsFile = new SanitizableFile(
                "db.properties", "db.password=secret123", "properties");

        String afterSecret = "db.password=***";
        SanitizationTransformation secretTransfo = new SanitizationTransformation(
                SanitizationRuleType.SECRET_REMOVAL, 1, "Secrets remplaces");
        SanitizationTransformation semgrepTransfo = new SanitizationTransformation(
                SanitizationRuleType.SEMGREP_SECURITY_SCAN, 0, "Semgrep : 0 finding(s)");

        when(secretSanitizer.apply("db.password=secret123")).thenReturn(afterSecret);
        when(secretSanitizer.report()).thenReturn(secretTransfo);
        when(semgrepScanSanitizer.applyToFile(
                new SanitizableFile("db.properties", afterSecret, "properties")))
                .thenReturn(afterSecret);
        when(semgrepScanSanitizer.report()).thenReturn(semgrepTransfo);

        List<SanitizedBundle> result = adapter.sanitizeFiles("bundle-props", List.of(propsFile));

        assertThat(result).hasSize(1);
        SanitizedBundle bundle = result.get(0);
        assertThat(bundle.sanitizedSource()).isEqualTo(afterSecret);
        assertThat(bundle.report().transformations())
                .extracting(SanitizationTransformation::ruleType)
                .containsExactly(SanitizationRuleType.SECRET_REMOVAL, SanitizationRuleType.SEMGREP_SECURITY_SCAN);
        verifyNoInteractions(sanitizationPort);
    }

    // --- Test consommateurs existants non impactes : SanitizationPort non modifie ---

    @Test
    void should_not_break_existing_sanitization_port_contract() {
        // Verifie que SanitizationPort n'est PAS appele pour un fichier non-Java
        SanitizableFile sqlFile = new SanitizableFile("schema.sql", "SELECT password FROM users;", "sql");

        String afterSecret = "SELECT *** FROM users;";
        SanitizationTransformation secretTransfo = new SanitizationTransformation(
                SanitizationRuleType.SECRET_REMOVAL, 1, "Secrets remplaces");
        SanitizationTransformation semgrepTransfo = new SanitizationTransformation(
                SanitizationRuleType.SEMGREP_SECURITY_SCAN, 0, "Semgrep : 0 finding(s)");

        when(secretSanitizer.apply(anyString())).thenReturn(afterSecret);
        when(secretSanitizer.report()).thenReturn(secretTransfo);
        when(semgrepScanSanitizer.applyToFile(
                new SanitizableFile("schema.sql", afterSecret, "sql")))
                .thenReturn(afterSecret);
        when(semgrepScanSanitizer.report()).thenReturn(semgrepTransfo);

        adapter.sanitizeFiles("bundle-sql", List.of(sqlFile));

        verifyNoInteractions(sanitizationPort);
    }

    // --- Test null bundleId ---

    @Test
    void should_throw_npe_when_bundle_id_is_null() {
        assertThatNullPointerException()
                .isThrownBy(() -> adapter.sanitizeFiles(null, List.of()))
                .withMessageContaining("bundleId must not be null");
    }

    // --- Test null files ---

    @Test
    void should_throw_npe_when_files_is_null() {
        assertThatNullPointerException()
                .isThrownBy(() -> adapter.sanitizeFiles("bundle-1", null))
                .withMessageContaining("files must not be null");
    }

    // --- Helpers ---

    private SanitizedBundle buildApprovedBundle(
            final String bundleId, final String ref, final String source) {
        SanitizationReport report = SanitizationReport.approved(bundleId, "1.0", List.of());
        return new SanitizedBundle(bundleId, ref, source, 10, "1.0", report);
    }
}
