package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceReaderPort;
import ff.ss.javaFxAuditStudio.domain.analysis.ControllerDependency;
import ff.ss.javaFxAuditStudio.domain.analysis.DeltaAnalysisSummary;
import ff.ss.javaFxAuditStudio.domain.analysis.DependencyKind;
import ff.ss.javaFxAuditStudio.domain.analysis.DetectionStatus;
import ff.ss.javaFxAuditStudio.domain.analysis.StateMachineInsight;
import ff.ss.javaFxAuditStudio.domain.analysis.StateTransition;
import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInput;
import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInputType;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionResult;
import ff.ss.javaFxAuditStudio.domain.rules.ParsingMode;
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

    private static final SourceReaderPort NO_OP_SOURCE_READER = ref -> Optional.empty();

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

        port = (controllerRef, javaContent) -> ExtractionResult.ast(List.of());
        service = new ClassifyResponsibilitiesService(port, NO_OP_PERSISTENCE, NO_OP_SOURCE_READER);

        result = service.handle("session-1", "com/example/MyController.java");

        assertThat(result.rules()).isEmpty();
        assertThat(result.uncertainRules()).isEmpty();
        assertThat(result.hasUncertainties()).isFalse();
        assertThat(result.parsingMode()).isEqualTo(ParsingMode.AST);
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

        port = (controllerRef, javaContent) ->
                ExtractionResult.ast(List.of(certainRule, uncertainRule1, uncertainRule2));
        service = new ClassifyResponsibilitiesService(port, NO_OP_PERSISTENCE, NO_OP_SOURCE_READER);

        result = service.handle("session-1", "com/example/MyController.java");

        assertThat(result.rules()).hasSize(1);
        assertThat(result.rules()).extracting(BusinessRule::ruleId).containsExactly("RG-001");
        assertThat(result.uncertainRules()).hasSize(2);
        assertThat(result.uncertainRules()).extracting(BusinessRule::ruleId)
                .containsExactlyInAnyOrder("RG-002", "RG-003");
        assertThat(result.hasUncertainties()).isTrue();
        assertThat(result.parsingMode()).isEqualTo(ParsingMode.AST);
    }

    @Test
    void handle_propagatesRegexFallbackMode() {
        BusinessRule rule;
        RuleExtractionPort port;
        ClassifyResponsibilitiesService service;
        ClassificationResult result;

        rule = buildRule("RG-001", false);

        port = (controllerRef, javaContent) ->
                ExtractionResult.regexFallback(List.of(rule), "Parse error at line 5");
        service = new ClassifyResponsibilitiesService(port, NO_OP_PERSISTENCE, NO_OP_SOURCE_READER);

        result = service.handle("session-fallback", "com/example/MyController.java");

        assertThat(result.parsingMode()).isEqualTo(ParsingMode.REGEX_FALLBACK);
        assertThat(result.parsingFallbackReason()).isEqualTo("Parse error at line 5");
        assertThat(result.rules()).hasSize(1);
    }

    @Test
    void handle_enrichesCachedClassificationWithLiveAnalysisAndDelta() {
        BusinessRule cachedSaveRule;
        BusinessRule cachedDeleteRule;
        BusinessRule liveSaveRule;
        BusinessRule liveCreateRule;
        ClassificationPersistencePort persistencePort;
        SourceReaderPort sourceReaderPort;
        RuleExtractionPort port;
        ClassifyResponsibilitiesService service;
        ClassificationResult result;

        cachedSaveRule = new BusinessRule(
                "RG-010",
                "Methode handler onSave : sauvegarde initiale",
                "com/example/MyController.java",
                0,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false);
        cachedDeleteRule = new BusinessRule(
                "RG-011",
                "Methode handler onDelete : suppression initiale",
                "com/example/MyController.java",
                0,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false);
        liveSaveRule = new BusinessRule(
                "RG-010-live",
                "Methode handler onSave : sauvegarde initiale",
                "com/example/MyController.java",
                0,
                ResponsibilityClass.BUSINESS,
                ExtractionCandidate.POLICY,
                false);
        liveCreateRule = new BusinessRule(
                "RG-012",
                "Methode handler onCreate : creation initiale",
                "com/example/MyController.java",
                0,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false);

        persistencePort = new ClassificationPersistencePort() {
            @Override
            public ClassificationResult save(final String sessionId, final ClassificationResult result) {
                return result;
            }

            @Override
            public Optional<ClassificationResult> findBySessionId(final String sessionId) {
                return Optional.of(new ClassificationResult(
                        "com/example/MyController.java",
                        List.of(cachedSaveRule, cachedDeleteRule),
                        List.of(),
                        ParsingMode.AST,
                        null,
                        0,
                        StateMachineInsight.absent(),
                        List.of(),
                        DeltaAnalysisSummary.none()));
            }
        };
        sourceReaderPort = ref -> Optional.of(new SourceInput(
                ref,
                SourceInputType.JAVA_CONTROLLER,
                "public class MyController {}"));
        port = (controllerRef, javaContent) -> ExtractionResult.ast(
                List.of(liveSaveRule, liveCreateRule),
                0,
                new StateMachineInsight(
                        DetectionStatus.CONFIRMED,
                        0.75d,
                        List.of("DRAFT", "SAVED"),
                        List.of(new StateTransition("DRAFT", "SAVED", "onSave"))),
                List.of(new ControllerDependency(
                        DependencyKind.SHARED_SERVICE,
                        "BillingService",
                        "billingService")));

        service = new ClassifyResponsibilitiesService(port, persistencePort, sourceReaderPort);

        result = service.handle("session-cache", "com/example/MyController.java");

        assertThat(result.rules()).containsExactly(cachedSaveRule, cachedDeleteRule);
        assertThat(result.stateMachine().status()).isEqualTo(DetectionStatus.CONFIRMED);
        assertThat(result.stateMachine().transitions()).hasSize(1);
        assertThat(result.dependencies()).containsExactly(new ControllerDependency(
                DependencyKind.SHARED_SERVICE,
                "BillingService",
                "billingService"));
        assertThat(result.deltaAnalysis()).isEqualTo(new DeltaAnalysisSummary(1, 1, 1));
    }

    @Test
    void handle_preservesAdvancedExtractionSignalsOnFreshClassification() {
        BusinessRule rule;
        RuleExtractionPort port;
        SourceReaderPort sourceReaderPort;
        ClassifyResponsibilitiesService service;
        ClassificationResult result;

        rule = buildRule("RG-020", false);
        sourceReaderPort = ref -> Optional.of(new SourceInput(
                ref,
                SourceInputType.JAVA_CONTROLLER,
                "public class MyController {}"));
        port = (controllerRef, javaContent) -> ExtractionResult.ast(
                List.of(rule),
                2,
                new StateMachineInsight(
                        DetectionStatus.POSSIBLE,
                        0.45d,
                        List.of("DRAFT"),
                        List.of()),
                List.of(new ControllerDependency(
                        DependencyKind.DIRECT_CONTROLLER,
                        "DetailsController",
                        "detailsController")));

        service = new ClassifyResponsibilitiesService(port, NO_OP_PERSISTENCE, sourceReaderPort);

        result = service.handle("session-advanced", "com/example/MyController.java");

        assertThat(result.excludedLifecycleMethodsCount()).isEqualTo(2);
        assertThat(result.stateMachine().status()).isEqualTo(DetectionStatus.POSSIBLE);
        assertThat(result.dependencies()).containsExactly(new ControllerDependency(
                DependencyKind.DIRECT_CONTROLLER,
                "DetailsController",
                "detailsController"));
        assertThat(result.deltaAnalysis()).isEqualTo(DeltaAnalysisSummary.none());
    }
}
