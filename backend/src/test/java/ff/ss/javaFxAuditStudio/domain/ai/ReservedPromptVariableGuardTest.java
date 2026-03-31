package ff.ss.javaFxAuditStudio.domain.ai;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ReservedPromptVariableGuardTest {

    private static SanitizedBundle fakeBundle() {
        return new SanitizedBundle("bid", "CtrlRef", "source", 100, "1.0", null);
    }

    @Test
    void construction_fails_when_extraContext_contains_sanitizedSource() {
        assertThatThrownBy(() -> new AiEnrichmentRequest(
                "req1", fakeBundle(), TaskType.NAMING, "enrichment-naming",
                Map.of("sanitizedSource", "INJECTED")))
                .isInstanceOf(ReservedPromptVariableException.class);
    }

    @Test
    void construction_fails_when_extraContext_contains_controllerRef() {
        assertThatThrownBy(() -> new AiEnrichmentRequest(
                "req2", fakeBundle(), TaskType.NAMING, "enrichment-naming",
                Map.of("controllerRef", "INJECTED")))
                .isInstanceOf(ReservedPromptVariableException.class);
    }

    @Test
    void construction_fails_when_extraContext_contains_bundleId() {
        assertThatThrownBy(() -> new AiEnrichmentRequest(
                "req3", fakeBundle(), TaskType.NAMING, "enrichment-naming",
                Map.of("bundleId", "INJECTED")))
                .isInstanceOf(ReservedPromptVariableException.class);
    }

    @Test
    void construction_succeeds_when_extraContext_is_empty() {
        assertThatNoException().isThrownBy(() -> new AiEnrichmentRequest(
                "req4", fakeBundle(), TaskType.NAMING, "enrichment-naming", Map.of()));
    }

    @Test
    void construction_succeeds_with_valid_context_for_ARTIFACT_REFINEMENT() {
        assertThatNoException().isThrownBy(() -> new AiEnrichmentRequest(
                "req5", fakeBundle(), TaskType.ARTIFACT_REFINEMENT, "artifact-refine",
                Map.of("artifactType", "USE_CASE", "refineInstruction", "ameliore le code",
                        "currentArtifactCode", "class Foo{}", "classifiedRules", "rules",
                        "screenContext", "ctx", "reclassificationFeedback", "none",
                        "projectReferencePatterns", "patterns")));
    }
}
