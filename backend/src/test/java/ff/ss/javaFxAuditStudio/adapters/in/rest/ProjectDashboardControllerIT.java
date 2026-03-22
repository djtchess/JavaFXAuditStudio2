package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.application.ports.in.GetProjectDashboardUseCase;
import ff.ss.javaFxAuditStudio.domain.workbench.ProjectDashboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IT MockMvc JAS-015 — ProjectDashboardController.
 * Utilise @SpringBootTest(MOCK) + profil "test" (H2, Flyway desactive).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ProjectDashboardControllerIT {

    @Autowired
    private WebApplicationContext wac;

    @MockitoBean
    private GetProjectDashboardUseCase getProjectDashboardUseCase;

    private MockMvc mockMvc;

    private static final String PROJECT_ID = "MyController";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void getDashboard_returns200_withCorrectStructure() throws Exception {
        ProjectDashboard dashboard = new ProjectDashboard(
                PROJECT_ID, 3, 1, 2,
                Map.of("UI", 5L, "BUSINESS", 3L),
                2L, 1L,
                List.of("UI", "BUSINESS"));
        when(getProjectDashboardUseCase.get(PROJECT_ID)).thenReturn(Optional.of(dashboard));

        mockMvc.perform(get("/api/v1/projects/{projectId}/dashboard", PROJECT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(PROJECT_ID))
                .andExpect(jsonPath("$.totalSessions").value(3))
                .andExpect(jsonPath("$.analysingCount").value(1))
                .andExpect(jsonPath("$.completedCount").value(2))
                .andExpect(jsonPath("$.uncertainCount").value(2))
                .andExpect(jsonPath("$.reclassifiedCount").value(1))
                .andExpect(jsonPath("$.rulesByCategory.UI").value(5))
                .andExpect(jsonPath("$.recommendedLotOrder[0]").value("UI"));
    }

    @Test
    void getDashboard_returns404_whenProjectUnknown() throws Exception {
        when(getProjectDashboardUseCase.get("inconnu")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/projects/{projectId}/dashboard", "inconnu"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listProjects_returns200_withProjectIds() throws Exception {
        when(getProjectDashboardUseCase.listProjects()).thenReturn(List.of("ControllerA", "ControllerB"));

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("ControllerA"))
                .andExpect(jsonPath("$[1]").value("ControllerB"));
    }

    @Test
    void listProjects_returns200_withEmptyList_whenNoProjects() throws Exception {
        when(getProjectDashboardUseCase.listProjects()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
