package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.ArtifactReviewResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de ReviewArtifactsService (JAS-030).
 *
 * <p>Le service lit le fichier source via Files.readString — en l'absence du fichier physique
 * le fallback retourne le controllerName. Le sanitizationPort est toujours mocke pour
 * intercepter l'appel avant envoi a l'IA.
 */
@ExtendWith(MockitoExtension.class)
class ReviewArtifactsServiceTest {

    @Mock
    private AnalysisSessionPort sessionPort;

    @Mock
    private ClassificationPersistencePort classificationPort;

    @Mock
    private AiEnrichmentPort aiEnrichmentPort;

    @Mock
    private SanitizationPort sanitizationPort;

    @Mock
    private SourceFileReaderPort sourceFileReaderPort;

    private ReviewArtifactsService service;

    @BeforeEach
    void setUp() {
        service = new ReviewArtifactsService(sessionPort, classificationPort, aiEnrichmentPort, sanitizationPort);
    }

    // --- Helpers ---

    private static AnalysisSession sessionWith(final String sessionId, final String controllerName) {
        return new AnalysisSession(sessionId, controllerName, null, AnalysisStatus.COMPLETED, Instant.now());
    }

    private static SanitizedBundle bundleFor(final String controllerRef) {
        return new SanitizedBundle("test-bundle", controllerRef, "source-sanitized", 1, "1.0", null);
    }

    private static BusinessRule ruleWith(
            final String ruleId,
            final ExtractionCandidate candidate,
            final boolean uncertain) {
        return new BusinessRule(
                ruleId,
                "Description de " + ruleId,
                "MyController.java",
                42,
                ResponsibilityClass.BUSINESS,
                candidate,
                uncertain);
    }

    private static ClassificationResult classificationWith(
            final List<BusinessRule> certain,
            final List<BusinessRule> uncertain) {
        return new ClassificationResult("MyController", certain, uncertain);
    }

    private static AiEnrichmentResult nominalResult(final Map<String, String> suggestions) {
        return new AiEnrichmentResult("req-test", false, "", suggestions, 10, LlmProvider.CLAUDE_CODE);
    }

    // ==========================================================================
    // Cas 1 : revue nominale avec regles — migrationScore retourne et degraded=false
    // ==========================================================================

    @Test
    void review_nominalWithRules_returnsMigrationScore() {
        AnalysisSession session = sessionWith("sess-nom", "MyController");
        BusinessRule rule = ruleWith("RG-001", ExtractionCandidate.USE_CASE, false);
        ClassificationResult classification = classificationWith(List.of(rule), List.of());

        when(sessionPort.findById("sess-nom")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-nom")).thenReturn(Optional.of(classification));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundleFor("MyController"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                nominalResult(Map.of("migrationScore", "75")));

        ArtifactReviewResult result = service.review("sess-nom");

        assertThat(result.degraded()).isFalse();
        assertThat(result.migrationScore()).isEqualTo(75);
    }

    // ==========================================================================
    // Cas 2 : session introuvable → IllegalArgumentException
    // ==========================================================================

    @Test
    void review_sessionNotFound_throwsIllegalArgumentException() {
        when(sessionPort.findById("unknown-session")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.review("unknown-session"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown-session");
    }

    // ==========================================================================
    // Cas 3 : pas de classification → retour degrade immediat (le service sort tot)
    // ==========================================================================

    @Test
    void review_noClassification_returnsDegradedResult() {
        AnalysisSession session = sessionWith("sess-no-classif", "MyController");

        when(sessionPort.findById("sess-no-classif")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-no-classif")).thenReturn(Optional.empty());

        ArtifactReviewResult result = service.review("sess-no-classif");

        assertThat(result.degraded()).isTrue();
        assertThat(result.migrationScore()).isEqualTo(-1);
        assertThat(result.degradationReason()).isNotBlank();
    }

    // ==========================================================================
    // Cas 4 : enrichmentPort retourne un resultat degrade → ArtifactReviewResult.degraded=true
    // ==========================================================================

    @Test
    void review_degradedAiResult_returnsDegradedReview() {
        AnalysisSession session = sessionWith("sess-degrad", "MyController");
        ClassificationResult classification = classificationWith(List.of(), List.of());

        when(sessionPort.findById("sess-degrad")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-degrad")).thenReturn(Optional.of(classification));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundleFor("MyController"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-degrad", "Circuit ouvert"));

        ArtifactReviewResult result = service.review("sess-degrad");

        assertThat(result.degraded()).isTrue();
        assertThat(result.migrationScore()).isEqualTo(-1);
    }

    // ==========================================================================
    // Cas 5 : sanitizationPort leve SanitizationRefusedException → resultat degrade
    // ==========================================================================

    @Test
    void review_sanitizationRefused_returnsDegradedReview() {
        AnalysisSession session = sessionWith("sess-sanit", "SensitiveController");
        ClassificationResult classification = classificationWith(List.of(), List.of());

        when(sessionPort.findById("sess-sanit")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-sanit")).thenReturn(Optional.of(classification));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenThrow(new SanitizationRefusedException("marqueur sensible detecte"));

        ArtifactReviewResult result = service.review("sess-sanit");

        assertThat(result.degraded()).isTrue();
        assertThat(result.migrationScore()).isEqualTo(-1);
        assertThat(result.degradationReason()).contains("Sanitisation refusee");
    }

    // ==========================================================================
    // Cas 6 : AI retourne suggestions.global avec lignes → globalSuggestions.size() == 3
    // ==========================================================================

    @Test
    void review_parsesGlobalSuggestions_asList() {
        AnalysisSession session = sessionWith("sess-global", "MyController");
        ClassificationResult classification = classificationWith(List.of(), List.of());

        when(sessionPort.findById("sess-global")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-global")).thenReturn(Optional.of(classification));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundleFor("MyController"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                nominalResult(Map.of("global", "line1\nline2\nline3")));

        ArtifactReviewResult result = service.review("sess-global");

        assertThat(result.degraded()).isFalse();
        assertThat(result.globalSuggestions()).hasSize(3);
        assertThat(result.globalSuggestions()).containsExactly("line1", "line2", "line3");
    }

    // ==========================================================================
    // Cas 7 : cle "uncertain_RG-007" dans suggestions → dans uncertainReclassifications
    // ==========================================================================

    @Test
    void review_parsesUncertainKeys_toUncertainReclassifications() {
        AnalysisSession session = sessionWith("sess-uncertain", "MyController");
        ClassificationResult classification = classificationWith(List.of(), List.of());

        when(sessionPort.findById("sess-uncertain")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-uncertain")).thenReturn(Optional.of(classification));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundleFor("MyController"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                nominalResult(Map.of("uncertain_RG-007", "Suggestion pour RG-007")));

        ArtifactReviewResult result = service.review("sess-uncertain");

        assertThat(result.degraded()).isFalse();
        assertThat(result.uncertainReclassifications()).containsKey("RG-007");
        assertThat(result.uncertainReclassifications().get("RG-007"))
                .isEqualTo("Suggestion pour RG-007");
    }

    // ==========================================================================
    // Cas 8 : cle "USE_CASE" dans suggestions → dans artifactReviews
    // ==========================================================================

    @Test
    void review_parsesArtifactKeys_toArtifactReviews() {
        AnalysisSession session = sessionWith("sess-artifact", "MyController");
        ClassificationResult classification = classificationWith(List.of(), List.of());

        when(sessionPort.findById("sess-artifact")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-artifact")).thenReturn(Optional.of(classification));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundleFor("MyController"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                nominalResult(Map.of("USE_CASE", "Revue de l'artifact UseCase")));

        ArtifactReviewResult result = service.review("sess-artifact");

        assertThat(result.degraded()).isFalse();
        assertThat(result.artifactReviews()).containsKey("USE_CASE");
        assertThat(result.artifactReviews().get("USE_CASE")).isEqualTo("Revue de l'artifact UseCase");
    }

    // ==========================================================================
    // Cas 9 : regle uncertain=true → extraContext contient "UNCERTAIN" dans classifiedRules
    // ==========================================================================

    @Test
    void formatRules_uncertain_addsWarningMarker() {
        AnalysisSession session = sessionWith("sess-fmt", "MyController");
        BusinessRule uncertainRule = ruleWith("RG-099", ExtractionCandidate.POLICY, true);
        ClassificationResult classification = classificationWith(List.of(), List.of(uncertainRule));

        when(sessionPort.findById("sess-fmt")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-fmt")).thenReturn(Optional.of(classification));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundleFor("MyController"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-fmt", "disabled"));

        service.review("sess-fmt");

        ArgumentCaptor<AiEnrichmentRequest> captor = ArgumentCaptor.forClass(AiEnrichmentRequest.class);
        verify(aiEnrichmentPort).enrich(captor.capture());

        String classifiedRules = (String) captor.getValue().extraContext().get("classifiedRules");
        assertThat(classifiedRules).contains("UNCERTAIN");
        assertThat(classifiedRules).contains("RG-099");
    }

    // ==========================================================================
    // Cas 10 : suggestions sans cle "migrationScore" → migrationScore == -1
    // ==========================================================================

    @Test
    void review_missingMigrationScore_returnsMinusOne() {
        AnalysisSession session = sessionWith("sess-noscore", "MyController");
        ClassificationResult classification = classificationWith(List.of(), List.of());

        when(sessionPort.findById("sess-noscore")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-noscore")).thenReturn(Optional.of(classification));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundleFor("MyController"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                nominalResult(Map.of("global", "une suggestion")));

        ArtifactReviewResult result = service.review("sess-noscore");

        assertThat(result.degraded()).isFalse();
        assertThat(result.migrationScore()).isEqualTo(-1);
    }

    // ==========================================================================
    // Complementaires : taskType transmis a l'IA, format du bundle, null sessionId
    // ==========================================================================

    @Test
    void review_setsTaskTypeArtifactReview() {
        AnalysisSession session = sessionWith("sess-task", "MyController");
        ClassificationResult classification = classificationWith(List.of(), List.of());

        when(sessionPort.findById("sess-task")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-task")).thenReturn(Optional.of(classification));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundleFor("MyController"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-task", "disabled"));

        service.review("sess-task");

        ArgumentCaptor<AiEnrichmentRequest> captor = ArgumentCaptor.forClass(AiEnrichmentRequest.class);
        verify(aiEnrichmentPort).enrich(captor.capture());
        assertThat(captor.getValue().taskType()).isEqualTo(TaskType.ARTIFACT_REVIEW);
    }

    @Test
    void review_nullSessionId_throwsNullPointerException() {
        assertThatThrownBy(() -> service.review(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void review_migrationScoreNotParseable_returnsMinusOne() {
        AnalysisSession session = sessionWith("sess-noparse", "MyController");
        ClassificationResult classification = classificationWith(List.of(), List.of());

        when(sessionPort.findById("sess-noparse")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-noparse")).thenReturn(Optional.of(classification));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundleFor("MyController"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                nominalResult(Map.of("migrationScore", "not-a-number")));

        ArtifactReviewResult result = service.review("sess-noparse");

        assertThat(result.degraded()).isFalse();
        assertThat(result.migrationScore()).isEqualTo(-1);
    }

    @Test
    void review_should_read_raw_source_via_source_file_reader_port_when_available() {
        ReviewArtifactsService serviceWithSourceReader = new ReviewArtifactsService(
                sessionPort,
                classificationPort,
                aiEnrichmentPort,
                sanitizationPort,
                sourceFileReaderPort);
        AnalysisSession session = sessionWith("sess-source", "C:/tmp/MyController.java");
        ClassificationResult classification = classificationWith(List.of(), List.of());

        when(sessionPort.findById("sess-source")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-source")).thenReturn(Optional.of(classification));
        when(sourceFileReaderPort.read("C:/tmp/MyController.java"))
                .thenReturn(Optional.of("public class MyController {}"));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(bundleFor("C:/tmp/MyController.java"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-source", "disabled"));

        serviceWithSourceReader.review("sess-source");

        verify(sourceFileReaderPort).read("C:/tmp/MyController.java");
        verify(sanitizationPort).sanitize(
                any(),
                eq("public class MyController {}"),
                eq("C:/tmp/MyController.java"));
    }

    @Test
    void review_should_fallback_to_controller_ref_when_source_file_reader_returns_empty() {
        ReviewArtifactsService serviceWithSourceReader = new ReviewArtifactsService(
                sessionPort,
                classificationPort,
                aiEnrichmentPort,
                sanitizationPort,
                sourceFileReaderPort);
        AnalysisSession session = sessionWith("sess-fallback", "C:/tmp/MissingController.java");
        ClassificationResult classification = classificationWith(List.of(), List.of());

        when(sessionPort.findById("sess-fallback")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-fallback")).thenReturn(Optional.of(classification));
        when(sourceFileReaderPort.read("C:/tmp/MissingController.java"))
                .thenReturn(Optional.empty());
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(bundleFor("C:/tmp/MissingController.java"));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-fallback", "disabled"));

        serviceWithSourceReader.review("sess-fallback");

        verify(sanitizationPort).sanitize(
                any(),
                eq("C:/tmp/MissingController.java"),
                eq("C:/tmp/MissingController.java"));
    }
}
