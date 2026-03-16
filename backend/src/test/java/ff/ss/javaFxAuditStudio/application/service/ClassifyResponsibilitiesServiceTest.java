package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ClassifyResponsibilitiesServiceTest {

    private static BusinessRule buildRule(final String ruleId, final boolean uncertain) {
        return new BusinessRule(
                ruleId,
                "Description de la regle " + ruleId,
                "com/example/MyController.java",
                0,
                ResponsibilityClass.BUSINESS,
                ExtractionCandidate.POLICY,
                uncertain);
    }

    private static final ClassificationPersistencePort NO_OP_PERSISTENCE = new ClassificationPersistencePort() {
        @Override
        public ClassificationResult save(String sessionId, ClassificationResult result) {
            return result;
        }

        @Override
        public Optional<ClassificationResult> findBySessionId(String sessionId) {
            return Optional.empty();
        }
    };

    @Test
    void handle_returnsEmptyClassification_whenPortReturnsNothing() {
        RuleExtractionPort port;
        ClassifyResponsibilitiesService service;
        ClassificationResult result;

        port = (controllerRef, javaContent) -> List.of();
        service = new ClassifyResponsibilitiesService(port, NO_OP_PERSISTENCE);

        result = service.handle("session-1", "com/example/MyController.java");

        assertThat(result.rules()).isEmpty();
        assertThat(result.uncertainRules()).isEmpty();
        assertThat(result.hasUncertainties()).isFalse();
    }

    @Test
    void handle_partitionsUncertainRules() {
        BusinessRule certainRule;
        BusinessRule uncertainRule1;
        BusinessRule uncertainRule2;
        RuleExtractionPort port;
        ClassifyResponsibilitiesService service;
        ClassificationResult result;

        certainRule = buildRule("RG-001", false);
        uncertainRule1 = buildRule("RG-002", true);
        uncertainRule2 = buildRule("RG-003", true);

        port = (controllerRef, javaContent) -> List.of(certainRule, uncertainRule1, uncertainRule2);
        service = new ClassifyResponsibilitiesService(port, NO_OP_PERSISTENCE);

        result = service.handle("session-1", "com/example/MyController.java");

        assertThat(result.rules()).hasSize(1);
        assertThat(result.rules()).extracting(BusinessRule::ruleId).containsExactly("RG-001");
        assertThat(result.uncertainRules()).hasSize(2);
        assertThat(result.uncertainRules()).extracting(BusinessRule::ruleId)
                .containsExactlyInAnyOrder("RG-002", "RG-003");
        assertThat(result.hasUncertainties()).isTrue();
    }
}
