package ff.ss.javaFxAuditStudio.application.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TokenEstimator;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ParsingMode;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmServiceSupportTest {

    @Mock
    private SanitizationPort sanitizationPort;

    @Mock
    private SourceFileReaderPort sourceFileReaderPort;

    @Test
    void readSourceFile_should_return_reader_content_when_available() {
        when(sourceFileReaderPort.read("MyController.java"))
                .thenReturn(java.util.Optional.of("public class MyController {}"));

        String result = LlmServiceSupport.readSourceFile("MyController.java", sourceFileReaderPort);

        assertThat(result).isEqualTo("public class MyController {}");
    }

    @Test
    void readSourceFile_should_fallback_to_controller_ref_when_reader_returns_empty() {
        when(sourceFileReaderPort.read("MissingController.java"))
                .thenReturn(java.util.Optional.empty());

        String result = LlmServiceSupport.readSourceFile("MissingController.java", sourceFileReaderPort);

        assertThat(result).isEqualTo("MissingController.java");
    }

    @Test
    void readSourceFile_should_fallback_to_controller_ref_when_port_is_null() {
        String result = LlmServiceSupport.readSourceFile("ControllerRef", null);

        assertThat(result).isEqualTo("ControllerRef");
    }

    @Test
    void readSourceFile_should_throw_when_controller_ref_is_null() {
        assertThatThrownBy(() -> LlmServiceSupport.readSourceFile(null, sourceFileReaderPort))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void buildBundle_should_delegate_to_sanitization_port_when_present() {
        SanitizedBundle expected = new SanitizedBundle("b1", "MyCtrl", "sanitized", 10, "1.0");
        when(sanitizationPort.sanitize("b1", "rawSource", "MyCtrl")).thenReturn(expected);

        SanitizedBundle result = LlmServiceSupport.buildBundle("b1", "rawSource", "MyCtrl", sanitizationPort);

        assertThat(result).isEqualTo(expected);
        verify(sanitizationPort).sanitize("b1", "rawSource", "MyCtrl");
    }

    @Test
    void buildBundle_should_build_minimal_bundle_when_sanitization_port_is_null() {
        String rawSource = "public class Foo {}";

        SanitizedBundle result = LlmServiceSupport.buildBundle("b2", rawSource, "Foo", null);

        assertThat(result.bundleId()).isEqualTo("b2");
        assertThat(result.controllerRef()).isEqualTo("Foo");
        assertThat(result.sanitizedSource()).isEqualTo(rawSource);
        assertThat(result.estimatedTokens()).isEqualTo(TokenEstimator.estimate(rawSource));
        assertThat(result.sanitizationVersion()).isEqualTo(LlmServiceSupport.SANITIZATION_VERSION);
    }

    @Test
    void buildBundle_should_throw_when_bundle_id_is_null() {
        assertThatThrownBy(() -> LlmServiceSupport.buildBundle(null, "source", "ref", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void buildBundle_should_throw_when_raw_source_is_null() {
        assertThatThrownBy(() -> LlmServiceSupport.buildBundle("id", null, "ref", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void buildBundle_should_throw_when_controller_ref_is_null() {
        assertThatThrownBy(() -> LlmServiceSupport.buildBundle("id", "source", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void formatRules_should_format_certain_rules() {
        BusinessRule rule = new BusinessRule(
                "RG-001",
                "Save patient data",
                "MyCtrl.java",
                42,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false);
        ClassificationResult classification = new ClassificationResult(
                "MyCtrl",
                List.of(rule),
                List.of(),
                ParsingMode.AST,
                null,
                0);

        String result = LlmServiceSupport.formatRules(classification);

        assertThat(result).isEqualTo("[RG-001] Save patient data (line 42) -> USE_CASE / APPLICATION");
    }

    @Test
    void formatRules_should_append_warning_marker_for_uncertain_rules() {
        BusinessRule uncertainRule = new BusinessRule(
                "RG-002",
                "Validate form",
                "MyCtrl.java",
                10,
                ResponsibilityClass.BUSINESS,
                ExtractionCandidate.POLICY,
                true);
        ClassificationResult classification = new ClassificationResult(
                "MyCtrl",
                List.of(),
                List.of(uncertainRule),
                ParsingMode.AST,
                null,
                0);

        String result = LlmServiceSupport.formatRules(classification);

        assertThat(result).contains("WARNING UNCERTAIN");
        assertThat(result).contains("[RG-002]");
    }

    @Test
    void formatRules_should_include_both_certain_and_uncertain_rules() {
        BusinessRule certain = new BusinessRule(
                "RG-001",
                "Load data",
                "Ctrl.java",
                5,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false);
        BusinessRule uncertain = new BusinessRule(
                "RG-002",
                "Check access",
                "Ctrl.java",
                15,
                ResponsibilityClass.BUSINESS,
                ExtractionCandidate.POLICY,
                true);
        ClassificationResult classification = new ClassificationResult(
                "Ctrl",
                List.of(certain),
                List.of(uncertain),
                ParsingMode.AST,
                null,
                0);

        String result = LlmServiceSupport.formatRules(classification);
        String[] lines = result.split("\n");

        assertThat(lines).hasSize(2);
        assertThat(lines[0]).startsWith("[RG-001]");
        assertThat(lines[1]).startsWith("[RG-002]").contains("WARNING UNCERTAIN");
    }

    @Test
    void formatRules_should_return_empty_string_when_no_rules() {
        ClassificationResult classification = new ClassificationResult(
                "Ctrl",
                List.of(),
                List.of(),
                ParsingMode.AST,
                null,
                0);

        String result = LlmServiceSupport.formatRules(classification);

        assertThat(result).isEmpty();
    }

    @Test
    void formatRules_should_throw_when_classification_is_null() {
        assertThatThrownBy(() -> LlmServiceSupport.formatRules(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void estimateTokens_should_return_zero_for_null() {
        assertThat(LlmServiceSupport.estimateTokens(null)).isZero();
    }

    @Test
    void estimateTokens_should_return_zero_for_blank() {
        assertThat(LlmServiceSupport.estimateTokens("   ")).isZero();
    }

    @Test
    void estimateTokens_should_use_named_divisor_heuristic() {
        String source = "a".repeat(100);

        assertThat(LlmServiceSupport.estimateTokens(source)).isEqualTo(TokenEstimator.estimate(source));
    }
}
