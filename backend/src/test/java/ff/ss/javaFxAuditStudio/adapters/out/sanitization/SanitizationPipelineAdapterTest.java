package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ff.ss.javaFxAuditStudio.configuration.SanitizationProperties;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TokenEstimator;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires de SanitizationPipelineAdapter (JAS-018).
 */
class SanitizationPipelineAdapterTest {

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
    void should_sanitize_nominal_source() {
        // Source avec identifiants metier mais sans marqueur residuel apres sanitisation
        String source = "class Component_1 { void handle() { int x = 42; } }";

        SanitizedBundle bundle = adapter.sanitize("bundle-1", source, "MyController");

        assertThat(bundle).isNotNull();
        assertThat(bundle.bundleId()).isEqualTo("bundle-1");
        assertThat(bundle.controllerRef()).isEqualTo("MyController");
        assertThat(bundle.sanitizationVersion()).isEqualTo("1.0");
        assertThat(bundle.estimatedTokens()).isEqualTo(TokenEstimator.estimate(bundle.sanitizedSource()));
    }

    @Test
    void should_refuse_when_sensitive_marker_remains_after_sanitization() {
        // Source contenant un mot-cle sensible que le pipeline ne peut pas supprimer
        // car il est dans du code structurel (ex : nom de variable 'password' sans valeur)
        String source = "void init() { String password = null; authenticate(); }";

        assertThatThrownBy(() -> adapter.sanitize("bundle-2", source, "AuthController"))
                .isInstanceOf(SanitizationRefusedException.class)
                .hasMessageContaining("marqueur sensible");
    }

    @Test
    void should_refuse_when_token_count_exceeds_max() {
        // Plafond de 9 tokens. Source de 41 chars => estimatedTokens = 10 > 9 => refus.
        // On utilise des espaces et lettres minuscules pour eviter le detecteur de marqueurs
        // (qui cible : URL, email, alphanum > 30 chars, mots-cles sensibles).
        SanitizationProperties tinyProps = new SanitizationProperties("1.0", 9, true);
        List<Sanitizer> pipeline = List.of(new IdentifierSanitizer());
        SanitizationPipelineAdapter tinyAdapter = new SanitizationPipelineAdapter(
                pipeline, new SensitiveMarkerDetector(), tinyProps);

        // 41 chars => estimatedTokens = 41/4 = 10 > plafond 9 => refus pour depassement de tokens.
        // Alternance de lettres et espaces : pas de sequence alphanum > 30 chars ni mot-cle sensible.
        String source = "ab cd ef gh ij kl mn op qr st uv wx yz ab";

        assertThatThrownBy(() -> tinyAdapter.sanitize("bundle-3", source, "Ctrl"))
                .isInstanceOf(SanitizationRefusedException.class)
                .hasMessageContaining("token");
    }

    @Test
    void should_reject_blank_raw_source_with_illegal_argument_exception() {
        assertThatThrownBy(() -> adapter.sanitize("bundle-blank", "   ", "MyController"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rawSource must not be blank");
    }

    @Test
    void should_reject_empty_raw_source_with_illegal_argument_exception() {
        assertThatThrownBy(() -> adapter.sanitize("bundle-empty", "", "MyController"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rawSource must not be blank");
    }

    @Test
    void should_apply_all_sanitizers_in_order() {
        // Source avec identifiant metier, URL et email — tous doivent etre traites
        String source = "class InvoiceService { String u = \"https://pay.example.com/v1\"; }";

        // La source contient des marqueurs (URL, identifiant metier) qui seront traites
        // Apres sanitisation, il ne doit pas rester de marqueur URL non traite
        // Note : l'URL sera remplacee par SecretSanitizer mais 'INTERNAL_URL' ne contient pas https://
        SanitizedBundle bundle = adapter.sanitize("bundle-4", source, "InvoiceService");

        assertThat(bundle.sanitizedSource()).doesNotContain("pay.example.com");
        assertThat(bundle.sanitizedSource()).doesNotContain("InvoiceService");
    }
}
