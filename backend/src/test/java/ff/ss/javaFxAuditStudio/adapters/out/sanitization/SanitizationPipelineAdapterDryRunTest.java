package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import ff.ss.javaFxAuditStudio.configuration.SanitizationProperties;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests unitaires du mode dry-run de SanitizationPipelineAdapter (AI-2).
 *
 * <p>Verifie que :
 * <ul>
 *   <li>CA-1 : previewTransformations() ne leve jamais SanitizationRefusedException.</li>
 *   <li>CA-1 : le rapport indique sensitiveMarkersFound=true si un marqueur subsiste.</li>
 *   <li>Un rapport dry-run contient les transformations collectees par le pipeline.</li>
 * </ul>
 */
class SanitizationPipelineAdapterDryRunTest {

    private static final SanitizationProperties DEFAULT_PROPS =
            new SanitizationProperties("1.0", 4000, true);

    private SanitizationPipelineAdapter adapter;

    @BeforeEach
    void setUp() {
        List<Sanitizer> pipeline = List.of(
                new IdentifierSanitizer(),
                new SecretSanitizer(),
                new CommentSanitizer(),
                new DataSubstitutionSanitizer());
        adapter = new SanitizationPipelineAdapter(
                pipeline, new SensitiveMarkerDetector(), DEFAULT_PROPS);
    }

    @Test
    void dryRun_should_never_throw_SanitizationRefusedException_when_sensitive_marker_present() {
        // Source contenant un mot-cle sensible qui ferait echouer sanitize()
        String source = "void init() { String password = null; authenticate(); }";

        assertThatCode(() -> adapter.previewTransformations("bundle-dry-1", source, "AuthController"))
                .doesNotThrowAnyException();
    }

    @Test
    void dryRun_should_report_sensitiveMarkersFound_when_sensitive_keyword_remains() {
        String source = "void init() { String password = null; authenticate(); }";

        SanitizationReport report = adapter.previewTransformations("bundle-dry-2", source, "AuthController");

        assertThat(report).isNotNull();
        assertThat(report.sensitiveMarkersFound()).isTrue();
        assertThat(report.approved()).isFalse();
    }

    @Test
    void dryRun_should_collect_transformations_from_all_sanitizers() {
        String source = "class Component_1 { void handle() { int x = 42; } }";

        SanitizationReport report = adapter.previewTransformations("bundle-dry-3", source, "MyController");

        assertThat(report).isNotNull();
        // Le pipeline comporte 4 sanitizers : chacun doit contribuer une entree
        assertThat(report.transformations()).hasSize(4);
    }

    @Test
    void dryRun_should_return_approved_report_when_no_sensitive_marker_remains() {
        // Source sans marqueur sensible residuel apres pipeline
        String source = "class Component_1 { void handle() { int x = 42; } }";

        SanitizationReport report = adapter.previewTransformations("bundle-dry-4", source, "MyController");

        assertThat(report.approved()).isTrue();
        assertThat(report.sensitiveMarkersFound()).isFalse();
        assertThat(report.bundleId()).isEqualTo("bundle-dry-4");
        assertThat(report.profileVersion()).isEqualTo("1.0");
    }

    @Test
    void dryRun_should_never_throw_when_token_count_exceeds_max() {
        // Meme avec un plafond de tokens tres bas, previewTransformations ne doit pas refuser
        SanitizationProperties tinyProps = new SanitizationProperties("1.0", 1, true);
        List<Sanitizer> pipeline = List.of(new IdentifierSanitizer());
        SanitizationPipelineAdapter tinyAdapter = new SanitizationPipelineAdapter(
                pipeline, new SensitiveMarkerDetector(), tinyProps);

        String source = "ab cd ef gh ij kl mn op qr st uv wx yz ab";

        assertThatCode(() -> tinyAdapter.previewTransformations("bundle-dry-5", source, "Ctrl"))
                .doesNotThrowAnyException();
    }
}
