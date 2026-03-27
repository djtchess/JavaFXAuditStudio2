package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ff.ss.javaFxAuditStudio.configuration.SanitizationProperties;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test d'integration unitaire de la chaine complete sanitisation (AI-4-T5/T6, JAS-018).
 *
 * <p>Instancie manuellement le pipeline complet (sans Spring context) et verifie
 * les contrats bout-en-bout : approbation, nettoyage des termes metier, refus
 * sur marqueur residuel, presence du rapport et des transformations.
 */
class SanitizationPipelineFullIT {

    private static final SanitizationProperties DEFAULT_PROPS =
            new SanitizationProperties("1.0", 4000, true);

    private SanitizationPipelineAdapter pipeline;

    @BeforeEach
    void setUp() {
        List<Sanitizer> sanitizers = List.of(
                new IdentifierSanitizer(),
                new SecretSanitizer(),
                new CommentSanitizer(),
                new DataSubstitutionSanitizer());
        pipeline = new SanitizationPipelineAdapter(
                sanitizers, new SensitiveMarkerDetector(), DEFAULT_PROPS);
    }

    /**
     * Cas 1 — Pipeline complet approuve.
     * Source avec noms metier et Javadoc contenant un email :
     * le pipeline sanitise tout et le rapport est approuve.
     */
    @Test
    void should_approve_source_with_business_names_and_javadoc_email() {
        // Javadoc contient un email -> bloc supprime par CommentSanitizer.
        // Les noms metier sont remplaces par IdentifierSanitizer.
        // L'email dans le Javadoc disparait avant que le detecteur ne l'inspecte.
        String source = "/** Contact: dev@example.com */\n"
                + "public class OrderProcessor {\n"
                + "    private final InvoiceRepository repo;\n"
                + "    public void run() { repo.save(null); }\n"
                + "}";

        SanitizedBundle bundle = pipeline.sanitize("it-bundle-1", source, "OrderProcessor");

        assertThat(bundle).isNotNull();
        assertThat(bundle.report().approved()).isTrue();
    }

    /**
     * Cas 2 — Noms metier absents de la sortie sanitisee.
     * Les suffixes metier reconnus ne doivent plus apparaitre dans le source sanitise.
     */
    @Test
    void should_not_contain_business_suffixes_after_sanitization() {
        String source = "public class PaymentManager {\n"
                + "    private final OrderRepository orders;\n"
                + "    public void pay() { orders.save(null); }\n"
                + "}";

        SanitizedBundle bundle = pipeline.sanitize("it-bundle-2", source, "PaymentManager");

        String sanitized = bundle.sanitizedSource();
        assertThat(sanitized).doesNotContain("PaymentManager");
        assertThat(sanitized).doesNotContain("OrderRepository");
        assertThat(sanitized).doesNotContain("Manager");
        assertThat(sanitized).doesNotContain("Repository");
    }

    /**
     * Cas 3 — Refus si marqueur sensible residuel.
     * Un mot-cle sensible sans valeur string (ex: variable nommee "apiKey") n'est pas
     * substitue par SecretSanitizer et reste dans la source sanitisee,
     * ce qui declenche SanitizationRefusedException.
     */
    @Test
    void should_throw_refused_exception_when_sensitive_keyword_remains() {
        // "apiKey" sans assignation string : SecretSanitizer ne l'efface pas.
        // Le SensitiveMarkerDetector le detecte et leve l'exception.
        String source = "public class Cfg { String apiKey = null; }";

        assertThatThrownBy(() -> pipeline.sanitize("it-bundle-3", source, "Cfg"))
                .isInstanceOf(SanitizationRefusedException.class)
                .hasMessageContaining("marqueur sensible");
    }

    /**
     * Cas 4 — Rapport inclus dans le bundle.
     * bundle.report() ne doit jamais etre null apres un sanitize() reussi.
     */
    @Test
    void should_include_non_null_report_in_bundle() {
        String source = "public class Component_1 { void run() { int x = 1; } }";

        SanitizedBundle bundle = pipeline.sanitize("it-bundle-4", source, "Component_1");

        assertThat(bundle.report()).isNotNull();
    }

    /**
     * Cas 5 — Transformations presentes dans le rapport.
     * La liste des transformations ne doit pas etre vide pour une source avec contenu metier.
     */
    @Test
    void should_have_non_empty_transformations_in_report() {
        // Source avec nom metier -> au moins la transformation IDENTIFIER_REPLACEMENT
        String source = "public class InvoiceService { void compute() {} }";

        SanitizedBundle bundle = pipeline.sanitize("it-bundle-5", source, "InvoiceService");

        assertThat(bundle.report().transformations()).isNotEmpty();
    }
}
