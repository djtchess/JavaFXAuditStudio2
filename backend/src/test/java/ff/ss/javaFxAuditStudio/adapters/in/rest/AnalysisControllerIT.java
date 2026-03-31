package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.application.ports.in.AnalysisOrchestrationUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.CartographyUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ClassifyResponsibilitiesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ExportArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceMigrationPlanUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ProduceRestitutionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionStatusHistoryPort;
import ff.ss.javaFxAuditStudio.domain.cartography.CartographyUnknown;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.cartography.FxmlComponent;
import ff.ss.javaFxAuditStudio.domain.cartography.HandlerBinding;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.ExportResult;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;
import ff.ss.javaFxAuditStudio.domain.migration.PlannedLot;
import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import ff.ss.javaFxAuditStudio.domain.workbench.OrchestratedAnalysisResult;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ParsingMode;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import ff.ss.javaFxAuditStudio.domain.restitution.ConfidenceLevel;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JAS-89 â€” Tests d'integration du controller REST AnalysisController.
 *
 * Utilise @SpringBootTest(webEnvironment = MOCK) + MockMvcBuilders.webAppContextSetup
 * car @WebMvcTest a ete retire de Spring Boot 4.0.3.
 *
 * Les ports et use cases sont bouchonnes via @MockitoBean (spring-test 7+).
 * Le profil "test" active H2 + ddl-auto=create-drop, evitant toute connexion PostgreSQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AnalysisControllerIT {

    @Autowired
    private WebApplicationContext wac;

    @MockitoBean
    private AnalysisSessionPort analysisSessionPort;

    @MockitoBean
    private AnalysisSessionStatusHistoryPort statusHistoryPort;

    @MockitoBean
    private AnalysisOrchestrationUseCase analysisOrchestrationUseCase;

    @MockitoBean
    private CartographyUseCase cartographyUseCase;

    @MockitoBean
    private ClassifyResponsibilitiesUseCase classifyResponsibilitiesUseCase;

    @MockitoBean
    private ProduceMigrationPlanUseCase produceMigrationPlanUseCase;

    @MockitoBean
    private GenerateArtifactsUseCase generateArtifactsUseCase;

    @MockitoBean
    private ProduceRestitutionUseCase produceRestitutionUseCase;

    @MockitoBean
    private ExportArtifactsUseCase exportArtifactsUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void submitSession_returns201_withSessionIdNonNull() throws Exception {
        String requestBody;
        String responseBody;

        requestBody = """
                {
                    "sessionName": "MonController",
                    "sourceFilePaths": [
                        "src/main/java/com/example/MyController.java",
                        "src/main/resources/fxml/MyController.fxml"
                    ]
                }
                """;

        when(analysisSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        responseBody = mockMvc.perform(post("/api/v1/analysis/sessions")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.sessionName").value("MonController"))
                .andExpect(jsonPath("$.controllerRef").value("src/main/java/com/example/MyController.java"))
                .andExpect(jsonPath("$.sourceSnippetRef").value("src/main/resources/fxml/MyController.fxml"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).contains("sessionId");
    }

    @Test
    void submitSession_returns400_whenSourceFilePathsIsEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/analysis/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                    "sessionName": "Audit vide",
                                    "sourceFilePaths": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void getSession_returns200_whenSessionExists() throws Exception {
        String sessionId;
        AnalysisSession session;

        sessionId = "session-detaillee-it";
        session = new AnalysisSession(
                sessionId,
                "com/example/MyController.java",
                "snippets/MyController.txt",
                AnalysisStatus.CARTOGRAPHING,
                Instant.now());

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(session));

        mockMvc.perform(get("/api/v1/analysis/sessions/{sessionId}", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.status").value("CARTOGRAPHING"))
                .andExpect(jsonPath("$.sessionName").value("com/example/MyController.java"))
                .andExpect(jsonPath("$.controllerRef").value("com/example/MyController.java"))
                .andExpect(jsonPath("$.sourceSnippetRef").value("snippets/MyController.txt"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void getSession_returns404_whenSessionNotFound() throws Exception {
        String sessionId;

        sessionId = "session-absente-it";
        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/analysis/sessions/{sessionId}", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void runPipeline_returns404_whenSessionNotFound() throws Exception {
        String sessionId;

        sessionId = "session-introuvable-it";
        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/analysis/sessions/{sessionId}/run", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void runPipeline_returns409_whenSessionInProgress() throws Exception {
        String sessionId;
        AnalysisSession sessionEnCours;

        sessionId = "session-en-cours-it";
        sessionEnCours = new AnalysisSession(
                sessionId,
                "Audit en cours",
                "com/example/MyController.java",
                null,
                AnalysisStatus.INGESTING,
                Instant.now());

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(sessionEnCours));

        mockMvc.perform(post("/api/v1/analysis/sessions/{sessionId}/run", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void runPipeline_returns200_whenSessionCreated() throws Exception {
        String sessionId;
        AnalysisSession sessionCreee;
        OrchestratedAnalysisResult orchestrationResult;
        ControllerCartography cartography;
        ClassificationResult classification;
        MigrationPlan migrationPlan;
        GenerationResult generationResult;
        RestitutionReport restitutionReport;

        sessionId = "session-prete-it";
        sessionCreee = new AnalysisSession(
                sessionId,
                "com/example/MyController.java",
                null,
                AnalysisStatus.CREATED,
                Instant.now());

        cartography = new ControllerCartography(
                "com/example/MyController.java",
                "src/main/resources/fxml/MyController.fxml",
                List.of(new FxmlComponent("saveButton", "Button", "onSave")),
                List.of(new HandlerBinding("onSave", "saveButton", "Button")),
                List.of(new CartographyUnknown("fx:id:unknownField", "Type non resolu")));

        classification = new ClassificationResult(
                "com/example/MyController.java",
                List.of(new BusinessRule(
                        "BR-1",
                        "Le controller orchestre la sauvegarde",
                        "src/main/java/com/example/MyController.java",
                        42,
                        ResponsibilityClass.APPLICATION,
                        ExtractionCandidate.USE_CASE,
                        false)),
                List.of(),
                ParsingMode.AST,
                null,
                0);

        migrationPlan = new MigrationPlan(
                "com/example/MyController.java",
                List.of(new PlannedLot(
                        1,
                        "Extraction du use case",
                        "Isoler le flux de sauvegarde",
                        List.of("saveButton", "onSave"),
                        List.of())),
                true);

        generationResult = new GenerationResult(
                "com/example/MyController.java",
                List.of(new CodeArtifact(
                        "artifact-1",
                        ArtifactType.USE_CASE,
                        1,
                        "SaveUseCase",
                        "public class SaveUseCase {}",
                        false)),
                List.of("Aucun avertissement"));

        restitutionReport = new RestitutionReport(
                new RestitutionSummary(
                        "com/example/MyController.java",
                        1,
                        0,
                        1,
                        0,
                        ConfidenceLevel.HIGH,
                        false),
                List.of(),
                List.of("Aucune inconnue"),
                List.of("Extraction cible identifiee"),
                "# Restitution\n\n## Synthese");

        orchestrationResult = new OrchestratedAnalysisResult(
                sessionId,
                AnalysisStatus.COMPLETED,
                cartography,
                classification,
                migrationPlan,
                generationResult,
                restitutionReport,
                List.of());

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(sessionCreee));
        when(analysisOrchestrationUseCase.orchestrate(sessionId)).thenReturn(orchestrationResult);

        mockMvc.perform(post("/api/v1/analysis/sessions/{sessionId}/run", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.finalStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.cartography.controllerRef").value("com/example/MyController.java"))
                .andExpect(jsonPath("$.classification.ruleCount").value(1))
                .andExpect(jsonPath("$.migrationPlan.compilable").value(true))
                .andExpect(jsonPath("$.generationResult.artifacts[0].className").value("SaveUseCase"))
                .andExpect(jsonPath("$.restitutionReport.markdown").value("# Restitution\n\n## Synthese"));
    }

    @Test
    void getCartography_returnsMetierContent() throws Exception {
        String sessionId;
        AnalysisSession session;
        ControllerCartography cartography;

        sessionId = "session-cartography-it";
        session = new AnalysisSession(
                sessionId,
                "com/example/MyController.java",
                "src/main/resources/fxml/MyController.fxml",
                AnalysisStatus.CARTOGRAPHING,
                Instant.now());
        cartography = new ControllerCartography(
                "com/example/MyController.java",
                "src/main/resources/fxml/MyController.fxml",
                List.of(new FxmlComponent("saveButton", "Button", "onSave")),
                List.of(new HandlerBinding("onSave", "saveButton", "Button")),
                List.of(new CartographyUnknown("fx:id:unknownField", "Type non resolu")));

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(session));
        when(cartographyUseCase.handle(sessionId, session.controllerName(), session.sourceSnippetRef()))
                .thenReturn(cartography);

        mockMvc.perform(get("/api/v1/analysis/sessions/{sessionId}/cartography", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerRef").value("com/example/MyController.java"))
                .andExpect(jsonPath("$.fxmlRef").value("src/main/resources/fxml/MyController.fxml"))
                .andExpect(jsonPath("$.components[0].fxId").value("saveButton"))
                .andExpect(jsonPath("$.components[0].componentType").value("Button"))
                .andExpect(jsonPath("$.handlers[0].methodName").value("onSave"))
                .andExpect(jsonPath("$.hasUnknowns").value(true));
    }

    @Test
    void getClassification_returnsMetierContent() throws Exception {
        String sessionId;
        AnalysisSession session;
        ClassificationResult classification;

        sessionId = "session-classification-it";
        session = new AnalysisSession(
                sessionId,
                "com/example/MyController.java",
                null,
                AnalysisStatus.CLASSIFYING,
                Instant.now());
        classification = new ClassificationResult(
                "com/example/MyController.java",
                List.of(new BusinessRule(
                        "BR-1",
                        "Le controller orchestre la sauvegarde",
                        "src/main/java/com/example/MyController.java",
                        42,
                        ResponsibilityClass.APPLICATION,
                        ExtractionCandidate.USE_CASE,
                        false)),
                List.of(new BusinessRule(
                        "BR-2",
                        "L'etat visuel est local",
                        "src/main/java/com/example/MyController.java",
                        55,
                        ResponsibilityClass.PRESENTATION,
                        ExtractionCandidate.VIEW_MODEL,
                        true)),
                ParsingMode.REGEX_FALLBACK,
                "JavaParser indisponible",
                2);

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(session));
        when(classifyResponsibilitiesUseCase.handle(sessionId, session.controllerName())).thenReturn(classification);

        mockMvc.perform(get("/api/v1/analysis/sessions/{sessionId}/classification", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerRef").value("com/example/MyController.java"))
                .andExpect(jsonPath("$.ruleCount").value(1))
                .andExpect(jsonPath("$.uncertainCount").value(1))
                .andExpect(jsonPath("$.parsingMode").value("REGEX_FALLBACK"))
                .andExpect(jsonPath("$.parsingFallbackReason").value("JavaParser indisponible"))
                .andExpect(jsonPath("$.excludedLifecycleMethodsCount").value(2))
                .andExpect(jsonPath("$.rules[0].ruleId").value("BR-1"))
                .andExpect(jsonPath("$.rules[0].responsibilityClass").value("APPLICATION"))
                .andExpect(jsonPath("$.rules[1].uncertain").value(true));
    }

    @Test
    void getMigrationPlan_returnsMetierContent() throws Exception {
        String sessionId;
        AnalysisSession session;
        MigrationPlan migrationPlan;

        sessionId = "session-plan-it";
        session = new AnalysisSession(
                sessionId,
                "com/example/MyController.java",
                null,
                AnalysisStatus.PLANNING,
                Instant.now());
        migrationPlan = new MigrationPlan(
                "com/example/MyController.java",
                List.of(new PlannedLot(
                        1,
                        "Extraction du use case",
                        "Isoler le flux de sauvegarde",
                        List.of("saveButton", "onSave"),
                        List.of())),
                true);

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(session));
        when(produceMigrationPlanUseCase.handle(sessionId, session.controllerName())).thenReturn(migrationPlan);

        mockMvc.perform(get("/api/v1/analysis/sessions/{sessionId}/plan", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerRef").value("com/example/MyController.java"))
                .andExpect(jsonPath("$.compilable").value(true))
                .andExpect(jsonPath("$.lots[0].lotNumber").value(1))
                .andExpect(jsonPath("$.lots[0].title").value("Extraction du use case"))
                .andExpect(jsonPath("$.lots[0].extractionCandidates[0]").value("saveButton"));
    }

    @Test
    void getArtifacts_returnsMetierContent() throws Exception {
        String sessionId;
        AnalysisSession session;
        GenerationResult generationResult;

        sessionId = "session-artifacts-it";
        session = new AnalysisSession(
                sessionId,
                "com/example/MyController.java",
                null,
                AnalysisStatus.GENERATING,
                Instant.now());
        generationResult = new GenerationResult(
                "com/example/MyController.java",
                List.of(new CodeArtifact(
                        "artifact-1",
                        ArtifactType.USE_CASE,
                        1,
                        "SaveUseCase",
                        "public class SaveUseCase {}",
                        false)),
                List.of("Aucun avertissement"));

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(session));
        when(generateArtifactsUseCase.handle(sessionId, session.controllerName())).thenReturn(generationResult);

        mockMvc.perform(get("/api/v1/analysis/sessions/{sessionId}/artifacts", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerRef").value("com/example/MyController.java"))
                .andExpect(jsonPath("$.warnings[0]").value("Aucun avertissement"))
                .andExpect(jsonPath("$.artifacts[0].artifactId").value("artifact-1"))
                .andExpect(jsonPath("$.artifacts[0].type").value("USE_CASE"))
                .andExpect(jsonPath("$.artifacts[0].className").value("SaveUseCase"))
                .andExpect(jsonPath("$.artifacts[0].generationStatus").value("OK"));
    }

    @Test
    void getReport_returnsMarkdownAndMetierContent() throws Exception {
        String sessionId;
        AnalysisSession session;
        RestitutionReport restitutionReport;

        sessionId = "session-report-it";
        session = new AnalysisSession(
                sessionId,
                "com/example/MyController.java",
                null,
                AnalysisStatus.REPORTING,
                Instant.now());
        restitutionReport = new RestitutionReport(
                new RestitutionSummary(
                        "com/example/MyController.java",
                        1,
                        0,
                        1,
                        0,
                        ConfidenceLevel.HIGH,
                        false),
                List.of(),
                List.of("Aucune inconnue"),
                List.of("Extraction cible identifiee"),
                "# Restitution");

        when(analysisSessionPort.findById(sessionId)).thenReturn(Optional.of(session));
        when(produceRestitutionUseCase.handle(sessionId, session.controllerName())).thenReturn(restitutionReport);

        mockMvc.perform(get("/api/v1/analysis/sessions/{sessionId}/report", sessionId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerRef").value("com/example/MyController.java"))
                .andExpect(jsonPath("$.ruleCount").value(1))
                .andExpect(jsonPath("$.artifactCount").value(1))
                .andExpect(jsonPath("$.confidence").value("HIGH"))
                .andExpect(jsonPath("$.isActionable").value(true))
                .andExpect(jsonPath("$.findings[0]").value("Extraction cible identifiee"))
                .andExpect(jsonPath("$.unknowns[0]").value("Aucune inconnue"))
                .andExpect(jsonPath("$.markdown").value("# Restitution"));
    }

    @Test
    void exportArtifacts_returnsExportResultContent() throws Exception {
        String sessionId;

        sessionId = "session-export-it";
        when(exportArtifactsUseCase.export(sessionId, "c:/tmp/export")).thenReturn(
                new ExportResult(
                        "c:/tmp/export",
                        List.of("SaveUseCase.java"),
                        List.of()));

        mockMvc.perform(post("/api/v1/analysis/sessions/{sessionId}/artifacts/export", sessionId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"targetDirectory":"c:/tmp/export"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDirectory").value("c:/tmp/export"))
                .andExpect(jsonPath("$.exportedFiles[0]").value("SaveUseCase.java"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }
}
