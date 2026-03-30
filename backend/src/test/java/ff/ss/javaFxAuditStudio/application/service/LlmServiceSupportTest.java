package ff.ss.javaFxAuditStudio.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import ff.ss.javaFxAuditStudio.domain.analysis.ControllerDependency;
import ff.ss.javaFxAuditStudio.domain.analysis.DeltaAnalysisSummary;
import ff.ss.javaFxAuditStudio.domain.analysis.DetectionStatus;
import ff.ss.javaFxAuditStudio.domain.analysis.StateMachineInsight;
import ff.ss.javaFxAuditStudio.domain.analysis.StateTransition;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.cartography.FxmlComponent;
import ff.ss.javaFxAuditStudio.domain.cartography.HandlerBinding;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import ff.ss.javaFxAuditStudio.domain.rules.ReclassificationAuditEntry;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LlmServiceSupportTest {

    private static ClassificationResult classification() {
        BusinessRule rule = new BusinessRule(
                "RG-001",
                "Orchestration du use case",
                "Controller.java",
                12,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false);
        StateMachineInsight stateMachine = new StateMachineInsight(
                DetectionStatus.CONFIRMED,
                0.82d,
                List.of("IDLE", "BUSY"),
                List.of(new StateTransition("IDLE", "BUSY", "onSave")));
        List<ControllerDependency> dependencies = List.of(
                new ControllerDependency(ff.ss.javaFxAuditStudio.domain.analysis.DependencyKind.SHARED_SERVICE, "PatientService", "save()"));
        return new ClassificationResult(
                "Controller",
                List.of(rule),
                List.of(),
                ff.ss.javaFxAuditStudio.domain.rules.ParsingMode.AST,
                null,
                0,
                stateMachine,
                dependencies,
                DeltaAnalysisSummary.none());
    }

    private static ControllerCartography cartography() {
        return new ControllerCartography(
                "Controller",
                "controller.fxml",
                List.of(new FxmlComponent("saveButton", "Button", "onSave")),
                List.of(new HandlerBinding("onSave", "#saveButton", "Button")),
                List.of());
    }

    @Test
    void formatScreenContext_includes_cartography_and_classification() {
        AnalysisSession session = new AnalysisSession(
                "sess-1",
                "Controller",
                "ControllerSnippet.java",
                AnalysisStatus.COMPLETED,
                Instant.parse("2026-03-26T10:00:00Z"));

        String context = LlmServiceSupport.formatScreenContext(session, classification(), cartography());

        assertThat(context).contains("sess-1");
        assertThat(context).contains("controller.fxml");
        assertThat(context).contains("stateMachine=CONFIRMED");
        assertThat(context).contains("dependencies=1");
    }

    @Test
    void formatReclassificationFeedback_collects_audit_entries() {
        ClassificationResult classification = classification();
        ReclassificationAuditPort port = Mockito.mock(ReclassificationAuditPort.class);
        ReclassificationAuditEntry entry = new ReclassificationAuditEntry(
                "audit-1",
                "sess-1",
                "RG-001",
                ResponsibilityClass.APPLICATION,
                ResponsibilityClass.BUSINESS,
                "mieux isole",
                Instant.parse("2026-03-26T10:10:00Z"));
        Mockito.when(port.findByAnalysisIdAndRuleId("sess-1", "RG-001")).thenReturn(List.of(entry));

        String feedback = LlmServiceSupport.formatReclassificationFeedback("sess-1", classification, port);

        assertThat(feedback).contains("RG-001");
        assertThat(feedback).contains("APPLICATION -> BUSINESS");
        assertThat(feedback).contains("mieux isole");
    }

    @Test
    void formatGeneratedArtifacts_includes_code_artifacts_and_warnings() {
        GenerationResult result = new GenerationResult(
                "Controller",
                List.of(new CodeArtifact(
                        "art-1",
                        ArtifactType.USE_CASE,
                        1,
                        "PatientUseCase",
                        "public interface PatientUseCase {}",
                        false)),
                List.of("warning-1"));

        String summary = LlmServiceSupport.formatGeneratedArtifacts(result);

        assertThat(summary).contains("PatientUseCase");
        assertThat(summary).contains("warning-1");
    }

    @Test
    void formatRuleSourceSnippets_extracts_matching_method_bodies() {
        ClassificationResult classification = new ClassificationResult(
                "Controller",
                List.of(new BusinessRule(
                        "RG-001",
                        "Methode garde isNouvelExamenOperationVerif : decision metier BUSINESS detectee",
                        "Controller.java",
                        10,
                        ResponsibilityClass.BUSINESS,
                        ExtractionCandidate.POLICY,
                        false)),
                List.of());
        String source = """
                class Controller {
                    private boolean isNouvelExamenOperationVerif() {
                        return !lsElementsConstatExamen.isEmpty() || !lsElementsConstatMateriel.isEmpty();
                    }
                }
                """;

        String snippets = LlmServiceSupport.formatRuleSourceSnippets(source, classification);

        assertThat(snippets).contains("isNouvelExamenOperationVerif");
        assertThat(snippets).contains("!lsElementsConstatExamen.isEmpty()");
    }
}
