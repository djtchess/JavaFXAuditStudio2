package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.application.ports.in.AdvancedAnalysisUseCase;
import ff.ss.javaFxAuditStudio.domain.analysis.ControllerFlowAnalysis;
import ff.ss.javaFxAuditStudio.domain.analysis.ProjectDependencyGraph;
import ff.ss.javaFxAuditStudio.domain.analysis.ProjectDeltaAnalysis;

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

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AdvancedAnalysisControllerIT {

    @Autowired
    private WebApplicationContext wac;

    @MockitoBean
    private AdvancedAnalysisUseCase advancedAnalysisUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void getControllerFlow_returnsStateMachineAndGuards() throws Exception {
        when(advancedAnalysisUseCase.analyzeControllerFlow("session-flow")).thenReturn(
                new ControllerFlowAnalysis(
                        "OrderController.java",
                        "OrderController",
                        true,
                        0.85d,
                        "CONFIRMED",
                        List.of("STATE"),
                        List.of(new ControllerFlowAnalysis.StateTransition(
                                "STATE",
                                "REVIEW",
                                "handleNext",
                                "if (state == OrderState.DRAFT)",
                                42)),
                        List.of("isReady"),
                        List.of("isVisible"),
                        List.of("STATE", "isReady", "isVisible"),
                        List.of()));

        mockMvc.perform(get("/api/v1/analysis/sessions/{sessionId}/flow", "session-flow")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stateMachineDetected").value(true))
                .andExpect(jsonPath("$.detectionLevel").value("CONFIRMED"))
                .andExpect(jsonPath("$.states[0]").value("STATE"))
                .andExpect(jsonPath("$.policyGuardCandidates[0]").value("isReady"))
                .andExpect(jsonPath("$.uiGuardMethods[0]").value("isVisible"));
    }

    @Test
    void getDependencies_returnsGraphContent() throws Exception {
        when(advancedAnalysisUseCase.analyzeProjectDependencies("project-1", List.of("A.java", "B.java"))).thenReturn(
                new ProjectDependencyGraph(
                        "project-1",
                        List.of(
                                new ProjectDependencyGraph.ControllerNode("A.java", "AController", List.of("BillingService:billingService"), 2, 0),
                                new ProjectDependencyGraph.ControllerNode("B.java", "BController", List.of("BillingService:billingService"), 0, 1)),
                        List.of(
                                new ProjectDependencyGraph.DependencyEdge("A.java", "B.java", ProjectDependencyGraph.DependencyType.SHARED_SERVICE, "shared-service:BillingService:billingService"),
                                new ProjectDependencyGraph.DependencyEdge("A.java", "B.java", ProjectDependencyGraph.DependencyType.DIRECT_CALL, "direct-call:BController")),
                        List.of("A.java", "B.java"),
                        List.of()));

        mockMvc.perform(post("/api/v1/projects/analysis/dependencies")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"projectId":"project-1","controllerRefs":["A.java","B.java"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controllers[0].controllerRef").value("A.java"))
                .andExpect(jsonPath("$.dependencies[0].type").value("SHARED_SERVICE"))
                .andExpect(jsonPath("$.recommendedOrder[1]").value("B.java"));
    }

    @Test
    void getDelta_returnsDeltaContent() throws Exception {
        when(advancedAnalysisUseCase.analyzeProjectDelta("project-1", List.of("A.java", "B.java"), List.of("A.java", "C.java"))).thenReturn(
                new ProjectDeltaAnalysis(
                        "project-1",
                        "A.java,B.java",
                        "A.java,C.java",
                        1,
                        1,
                        1,
                        0,
                        List.of(
                                new ProjectDeltaAnalysis.ControllerDelta(
                                        "A.java",
                                        ProjectDeltaAnalysis.DeltaStatus.MODIFIED,
                                        List.of("Rule beta"),
                                        List.of("Rule alpha"),
                                        List.of(),
                                        List.of(),
                                        List.of()),
                                new ProjectDeltaAnalysis.ControllerDelta(
                                        "B.java",
                                        ProjectDeltaAnalysis.DeltaStatus.REMOVED,
                                        List.of(),
                                        List.of("Rule omega"),
                                        List.of(),
                                        List.of(),
                                        List.of()),
                                new ProjectDeltaAnalysis.ControllerDelta(
                                        "C.java",
                                        ProjectDeltaAnalysis.DeltaStatus.NEW,
                                        List.of("Rule gamma"),
                                        List.of(),
                                        List.of(),
                                        List.of(),
                                        List.of())),
                        List.of()));

        mockMvc.perform(post("/api/v1/projects/analysis/delta")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"projectId":"project-1","baselineControllerRefs":["A.java","B.java"],"currentControllerRefs":["A.java","C.java"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modifiedControllers").value(1))
                .andExpect(jsonPath("$.controllerDeltas[0].status").value("MODIFIED"))
                .andExpect(jsonPath("$.controllerDeltas[2].status").value("NEW"));
    }
}
