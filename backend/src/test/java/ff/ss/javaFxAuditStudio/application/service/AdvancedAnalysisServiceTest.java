package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceReaderPort;
import ff.ss.javaFxAuditStudio.configuration.AnalysisProperties;
import ff.ss.javaFxAuditStudio.domain.analysis.ControllerFlowAnalysis;
import ff.ss.javaFxAuditStudio.domain.analysis.ProjectDependencyGraph;
import ff.ss.javaFxAuditStudio.domain.analysis.ProjectDeltaAnalysis;
import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInput;
import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInputType;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionResult;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdvancedAnalysisServiceTest {

    @Test
    void analyzeControllerFlow_detecteStateMachine_policyEtUiGuards() {
        Map<String, String> sources = Map.of(
                "OrderController.java",
                """
                public class OrderController {
                    private OrderState state;

                    private boolean isVisible() {
                        return button != null;
                    }

                    private boolean isReady() {
                        return state == OrderState.READY;
                    }

                    public void handleNext() {
                        if (state == OrderState.DRAFT) {
                            state = OrderState.REVIEW;
                        }
                    }

                    public void handleApprove() {
                        switch (state) {
                            case REVIEW -> state = OrderState.APPROVED;
                            default -> state = OrderState.REVIEW;
                        }
                    }
                }
                """);

        AnalysisSessionPort analysisSessionPort = mock(AnalysisSessionPort.class);
        when(analysisSessionPort.findById("session-order")).thenReturn(Optional.of(new AnalysisSession(
                "session-order",
                "OrderController.java",
                null,
                AnalysisStatus.CREATED,
                Instant.parse("2026-03-26T10:00:00Z"))));

        AdvancedAnalysisService service = new AdvancedAnalysisService(
                analysisSessionPort,
                sourceReaderPort(sources),
                (ref, content) -> ExtractionResult.ast(List.of()),
                new AnalysisProperties(null, null));

        ControllerFlowAnalysis flow = service.analyzeControllerFlow("session-order");

        assertThat(flow.stateMachineDetected()).isTrue();
        assertThat(flow.detectionLevel()).isEqualTo("CONFIRMED");
        assertThat(flow.stateMachineConfidence()).isGreaterThanOrEqualTo(0.60d);
        assertThat(flow.states()).contains("STATE");
        assertThat(flow.policyGuardCandidates()).contains("isReady");
        assertThat(flow.uiGuardMethods()).contains("isVisible");
        assertThat(flow.transitions()).isNotEmpty();
    }

    @Test
    void analyzeProjectDependencies_detecteSharedServiceAndDirectCall() {
        Map<String, String> sources = Map.of(
                "FirstController.java",
                """
                public class FirstController {
                    @Autowired private BillingService billingService;

                    public void run() {
                        billingService.save();
                        new SecondController();
                    }
                }
                """,
                "SecondController.java",
                """
                public class SecondController {
                    @Autowired private BillingService billingService;

                    public void run() {
                        billingService.load();
                    }
                }
                """);

        AdvancedAnalysisService service = new AdvancedAnalysisService(
                mock(AnalysisSessionPort.class),
                sourceReaderPort(sources),
                (ref, content) -> ExtractionResult.ast(List.of(
                        new BusinessRule(
                                ref + "-rule",
                                "Rule " + ref,
                                ref,
                                1,
                                ResponsibilityClass.BUSINESS,
                                ExtractionCandidate.POLICY,
                                false))),
                new AnalysisProperties(null, null));

        ProjectDependencyGraph graph = service.analyzeProjectDependencies(
                "project-1",
                List.of("FirstController.java", "SecondController.java"));

        assertThat(graph.controllers()).hasSize(2);
        assertThat(graph.dependencies()).anyMatch(edge -> edge.type() == ProjectDependencyGraph.DependencyType.SHARED_SERVICE);
        assertThat(graph.dependencies()).anyMatch(edge -> edge.type() == ProjectDependencyGraph.DependencyType.DIRECT_CALL);
        assertThat(graph.recommendedOrder()).containsExactly("FirstController.java", "SecondController.java");
        assertThat(graph.controllers().get(0).outgoingDependencies()).isGreaterThanOrEqualTo(1);
        assertThat(graph.controllers().get(1).incomingDependencies()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void analyzeProjectDelta_detecteNewRemovedEtModifiedControllers() {
        Map<String, ArrayDeque<String>> sources = new HashMap<>();
        sources.put("AController.java", new ArrayDeque<>(List.of(
                """
                public class AController {
                    // RULE_ALPHA
                }
                """,
                """
                public class AController {
                    // RULE_BETA
                }
                """)));
        sources.put("BController.java", new ArrayDeque<>(List.of(
                """
                public class BController {
                    // RULE_OMEGA
                }
                """)));
        sources.put("CController.java", new ArrayDeque<>(List.of(
                """
                public class CController {
                    // RULE_GAMMA
                }
                """)));

        AdvancedAnalysisService service = new AdvancedAnalysisService(
                mock(AnalysisSessionPort.class),
                queuedSourceReaderPort(sources),
                (ref, content) -> extractionFor(ref, content),
                new AnalysisProperties(null, null));

        ProjectDeltaAnalysis delta = service.analyzeProjectDelta(
                "project-1",
                List.of("AController.java", "BController.java"),
                List.of("AController.java", "CController.java"));

        assertThat(delta.newControllers()).isEqualTo(1);
        assertThat(delta.removedControllers()).isEqualTo(1);
        assertThat(delta.modifiedControllers()).isEqualTo(1);
        assertThat(delta.unchangedControllers()).isEqualTo(0);
        assertThat(delta.controllerDeltas()).anyMatch(entry -> entry.controllerRef().equals("AController.java")
                && entry.status() == ProjectDeltaAnalysis.DeltaStatus.MODIFIED);
        assertThat(delta.controllerDeltas()).anyMatch(entry -> entry.controllerRef().equals("BController.java")
                && entry.status() == ProjectDeltaAnalysis.DeltaStatus.REMOVED);
        assertThat(delta.controllerDeltas()).anyMatch(entry -> entry.controllerRef().equals("CController.java")
                && entry.status() == ProjectDeltaAnalysis.DeltaStatus.NEW);
    }

    private static SourceReaderPort sourceReaderPort(final Map<String, String> sources) {
        return ref -> Optional.ofNullable(sources.get(ref))
                .map(content -> new SourceInput(ref, SourceInputType.JAVA_CONTROLLER, content));
    }

    private static SourceReaderPort queuedSourceReaderPort(final Map<String, ArrayDeque<String>> sources) {
        return ref -> {
            ArrayDeque<String> queue = sources.get(ref);
            if (queue == null || queue.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new SourceInput(ref, SourceInputType.JAVA_CONTROLLER, queue.removeFirst()));
        };
    }

    private static ExtractionResult extractionFor(final String ref, final String content) {
        if (content.contains("RULE_BETA")) {
            return ruleResult(ref, "beta");
        }
        if (content.contains("RULE_OMEGA")) {
            return ruleResult(ref, "omega");
        }
        if (content.contains("RULE_GAMMA")) {
            return ruleResult(ref, "gamma");
        }
        return ruleResult(ref, "alpha");
    }

    private static ExtractionResult ruleResult(final String ref, final String suffix) {
        return ExtractionResult.ast(List.of(
                new BusinessRule(
                        ref + "-" + suffix,
                        "Rule " + suffix,
                        ref,
                        1,
                        ResponsibilityClass.BUSINESS,
                        ExtractionCandidate.POLICY,
                        false)));
    }
}
