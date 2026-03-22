package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.out.ProjectDashboardPort;
import ff.ss.javaFxAuditStudio.domain.workbench.ProjectDashboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TU JAS-015 — GetProjectDashboardService.
 * Verifie le comportement du service sans contexte Spring.
 */
class GetProjectDashboardServiceTest {

    private ProjectDashboardPort port;
    private GetProjectDashboardService service;

    @BeforeEach
    void setUp() {
        port = mock(ProjectDashboardPort.class);
        service = new GetProjectDashboardService(port);
    }

    @Test
    void get_returnsEmpty_whenProjectUnknown() {
        when(port.computeDashboard("inconnu")).thenReturn(Optional.empty());

        Optional<ProjectDashboard> result = service.get("inconnu");

        assertThat(result).isEmpty();
    }

    @Test
    void get_returnsDashboard_whenProjectExists() {
        ProjectDashboard dashboard = new ProjectDashboard(
                "MyController",
                3,
                1,
                2,
                Map.of("UI", 5L, "BUSINESS", 3L),
                2L,
                1L,
                List.of("UI", "BUSINESS"));
        when(port.computeDashboard("MyController")).thenReturn(Optional.of(dashboard));

        Optional<ProjectDashboard> result = service.get("MyController");

        assertThat(result).isPresent();
        assertThat(result.get().projectId()).isEqualTo("MyController");
        assertThat(result.get().totalSessions()).isEqualTo(3);
        assertThat(result.get().analysingCount()).isEqualTo(1);
        assertThat(result.get().completedCount()).isEqualTo(2);
        assertThat(result.get().uncertainCount()).isEqualTo(2L);
        assertThat(result.get().reclassifiedCount()).isEqualTo(1L);
        assertThat(result.get().recommendedLotOrder()).containsExactly("UI", "BUSINESS");
    }

    @Test
    void listProjects_delegatesToPort() {
        when(port.findAllProjectIds()).thenReturn(List.of("ControllerA", "ControllerB"));

        List<String> result = service.listProjects();

        assertThat(result).containsExactly("ControllerA", "ControllerB");
    }
}
