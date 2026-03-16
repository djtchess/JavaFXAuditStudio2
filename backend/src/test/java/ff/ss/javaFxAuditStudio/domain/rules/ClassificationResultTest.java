package ff.ss.javaFxAuditStudio.domain.rules;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ClassificationResultTest {

    private static BusinessRule ruleWith(final String ruleId, final ExtractionCandidate candidate, final boolean uncertain) {
        return new BusinessRule(
                ruleId,
                "Description de la regle " + ruleId,
                "com/example/MyController.java",
                0,
                ResponsibilityClass.BUSINESS,
                candidate,
                uncertain);
    }

    @Test
    void hasUncertainties_returnsFalse_whenNone() {
        BusinessRule certainRule;
        ClassificationResult result;

        certainRule = ruleWith("RG-001", ExtractionCandidate.POLICY, false);
        result = new ClassificationResult("com/example/MyController.java", List.of(certainRule), List.of());

        assertThat(result.hasUncertainties()).isFalse();
    }

    @Test
    void hasUncertainties_returnsTrue_whenPresent() {
        BusinessRule uncertainRule;
        ClassificationResult result;

        uncertainRule = ruleWith("RG-002", ExtractionCandidate.USE_CASE, true);
        result = new ClassificationResult("com/example/MyController.java", List.of(), List.of(uncertainRule));

        assertThat(result.hasUncertainties()).isTrue();
    }

    @Test
    void candidates_returnsFilteredByType() {
        BusinessRule policyRule;
        BusinessRule useCaseRule;
        BusinessRule anotherPolicyRule;
        ClassificationResult result;
        List<BusinessRule> policyCandidates;

        policyRule = ruleWith("RG-001", ExtractionCandidate.POLICY, false);
        useCaseRule = ruleWith("RG-002", ExtractionCandidate.USE_CASE, false);
        anotherPolicyRule = ruleWith("RG-003", ExtractionCandidate.POLICY, false);
        result = new ClassificationResult(
                "com/example/MyController.java",
                List.of(policyRule, useCaseRule, anotherPolicyRule),
                List.of());

        policyCandidates = result.candidates(ExtractionCandidate.POLICY);

        assertThat(policyCandidates).hasSize(2);
        assertThat(policyCandidates).extracting(BusinessRule::ruleId)
                .containsExactlyInAnyOrder("RG-001", "RG-003");
    }

    @Test
    void candidates_throwsNullPointerException_whenTypeNull() {
        ClassificationResult result;

        result = new ClassificationResult("com/example/MyController.java", List.of(), List.of());

        assertThatNullPointerException()
                .isThrownBy(() -> result.candidates(null))
                .withMessageContaining("type");
    }
}
