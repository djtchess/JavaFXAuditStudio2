package ff.ss.javaFxAuditStudio.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AiArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.MigrationPlanPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ProjectReferencePatternPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.cartography.FxmlComponent;
import ff.ss.javaFxAuditStudio.domain.cartography.HandlerBinding;
import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;
import ff.ss.javaFxAuditStudio.domain.migration.PlannedLot;
import ff.ss.javaFxAuditStudio.domain.migration.RegressionRisk;
import ff.ss.javaFxAuditStudio.domain.migration.RiskLevel;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ReclassificationAuditEntry;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptContextPropagationTest {

    @Mock
    private AnalysisSessionPort sessionPort;

    @Mock
    private ClassificationPersistencePort classificationPort;

    @Mock
    private CartographyPersistencePort cartographyPort;

    @Mock
    private ReclassificationAuditPort reclassificationAuditPort;

    @Mock
    private AiEnrichmentPort aiEnrichmentPort;

    @Mock
    private AiArtifactPersistencePort aiArtifactPersistencePort;

    @Mock
    private ProjectReferencePatternPort projectReferencePatternPort;

    @Mock
    private MigrationPlanPersistencePort migrationPlanPersistencePort;

    @Mock
    private SanitizationPort sanitizationPort;

    @Mock
    private SourceFileReaderPort sourceFileReaderPort;

    private AnalysisSession session;
    private ClassificationResult classification;

    @BeforeEach
    void setUp() {
        session = new AnalysisSession(
                "sess-ctx",
                "Controller",
                "Controller.java",
                AnalysisStatus.COMPLETED,
                Instant.now());
        classification = classification();
        when(sessionPort.findById("sess-ctx")).thenReturn(Optional.of(session));
        when(classificationPort.findBySessionId("sess-ctx")).thenReturn(Optional.of(classification));
        when(cartographyPort.findBySessionId("sess-ctx")).thenReturn(Optional.of(cartography()));
        when(reclassificationAuditPort.findByAnalysisIdAndRuleId("sess-ctx", "RG-001"))
                .thenReturn(List.of(reclassification()));
        when(sourceFileReaderPort.read("Controller")).thenReturn(Optional.of("class Controller {}"));
        when(sanitizationPort.sanitize(any(), any(), any()))
                .thenReturn(new SanitizedBundle(
                        "bundle-ctx",
                        "Controller",
                        "import javafx.fxml.FXML;\nclass Controller {\n    @FXML\n    public void onSave() {\n        patientService.save();\n    }\n}",
                        10,
                        "1.0",
                        null));
        when(aiEnrichmentPort.enrich(any())).thenReturn(
                AiEnrichmentResult.degraded("req-ctx", "disabled"));
    }

    @Test
    void generation_service_propagates_screen_context_and_feedback() {
        when(projectReferencePatternPort.findAll()).thenReturn(List.of(projectPattern()));
        when(migrationPlanPersistencePort.findBySessionId("sess-ctx"))
                .thenReturn(Optional.of(migrationPlan()));

        AiSpringBootGenerationService service = new AiSpringBootGenerationService(
                sessionPort,
                classificationPort,
                cartographyPort,
                reclassificationAuditPort,
                aiArtifactPersistencePort,
                projectReferencePatternPort,
                migrationPlanPersistencePort,
                aiEnrichmentPort,
                sanitizationPort,
                sourceFileReaderPort);

        service.generate("sess-ctx");

        ArgumentCaptor<AiEnrichmentRequest> captor = ArgumentCaptor.forClass(AiEnrichmentRequest.class);
        verify(aiEnrichmentPort).enrich(captor.capture());
        assertThat(captor.getValue().extraContext()).containsKeys(
                "screenContext",
                "migrationPlan",
                "ruleSourceSnippets",
                "reclassificationFeedback",
                "projectReferencePatterns");
        assertThat(captor.getValue().extraContext().get("screenContext").toString())
                .contains("saveButton")
                .contains("onSave");
        assertThat(captor.getValue().extraContext().get("migrationPlan").toString())
                .contains("Lot 3")
                .contains("Handlers metier");
        assertThat(captor.getValue().extraContext().get("ruleSourceSnippets").toString())
                .contains("onSave")
                .contains("patientService.save()");
        assertThat(captor.getValue().extraContext().get("projectReferencePatterns").toString())
                .contains("USE_CASE")
                .contains("ReferenceUseCase");
    }

    @Test
    void review_service_propagates_screen_context_and_feedback() {
        ReviewArtifactsService service = new ReviewArtifactsService(
                sessionPort,
                classificationPort,
                cartographyPort,
                reclassificationAuditPort,
                aiEnrichmentPort,
                sanitizationPort,
                sourceFileReaderPort);

        service.review("sess-ctx");

        ArgumentCaptor<AiEnrichmentRequest> captor = ArgumentCaptor.forClass(AiEnrichmentRequest.class);
        verify(aiEnrichmentPort).enrich(captor.capture());
        assertThat(captor.getValue().extraContext()).containsKeys("screenContext", "reclassificationFeedback");
        assertThat(captor.getValue().extraContext().get("screenContext").toString())
                .contains("saveButton")
                .contains("onSave");
    }

    private static ClassificationResult classification() {
        BusinessRule rule = new BusinessRule(
                "RG-001",
                "Methode handler onSave : responsabilite APPLICATION detectee [complexite=1]",
                "Controller.java",
                12,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false);
        return new ClassificationResult("Controller", List.of(rule), List.of());
    }

    private static ControllerCartography cartography() {
        return new ControllerCartography(
                "Controller",
                "controller.fxml",
                List.of(new FxmlComponent("saveButton", "Button", "onSave")),
                List.of(new HandlerBinding("onSave", "#saveButton", "Button")),
                List.of());
    }

    private static ReclassificationAuditEntry reclassification() {
        return new ReclassificationAuditEntry(
                "audit-1",
                "sess-ctx",
                "RG-001",
                ResponsibilityClass.APPLICATION,
                ResponsibilityClass.BUSINESS,
                "plus metier",
                Instant.now());
    }

    private static ProjectReferencePattern projectPattern() {
        return new ProjectReferencePattern(
                "pattern-1",
                "USE_CASE",
                "ReferenceUseCase",
                "package ff.example.usecase;\npublic interface ReferenceUseCase {}",
                Instant.now());
    }

    private static MigrationPlan migrationPlan() {
        return new MigrationPlan(
                "Controller",
                List.of(new PlannedLot(
                        3,
                        "Handlers metier",
                        "Migration des handlers lourds",
                        List.of("USE_CASE", "POLICY"),
                        List.of(new RegressionRisk(
                                "Regression sur les handlers",
                                RiskLevel.MEDIUM,
                                "Tests de non-regression")))),
                true);
    }
}
