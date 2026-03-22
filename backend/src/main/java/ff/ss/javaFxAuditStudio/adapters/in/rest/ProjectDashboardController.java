package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ProjectDashboardResponse;
import ff.ss.javaFxAuditStudio.application.ports.in.GetProjectDashboardUseCase;
import ff.ss.javaFxAuditStudio.domain.workbench.ProjectDashboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller REST pour JAS-015 — tableau de bord projet.
 * GET /api/v1/projects/{projectId}/dashboard
 * GET /api/v1/projects
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectDashboardController {

    private static final Logger log = LoggerFactory.getLogger(ProjectDashboardController.class);

    private final GetProjectDashboardUseCase useCase;

    public ProjectDashboardController(final GetProjectDashboardUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{projectId}/dashboard")
    public ResponseEntity<ProjectDashboardResponse> getDashboard(
            @PathVariable final String projectId) {
        log.debug("[dashboard] GET /api/v1/projects/{}/dashboard", projectId);
        return useCase.get(projectId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<String>> listProjects() {
        log.debug("[dashboard] GET /api/v1/projects");
        return ResponseEntity.ok(useCase.listProjects());
    }

    private ProjectDashboardResponse toResponse(final ProjectDashboard d) {
        return new ProjectDashboardResponse(
                d.projectId(),
                d.totalSessions(),
                d.analysingCount(),
                d.completedCount(),
                d.rulesByCategory(),
                d.uncertainCount(),
                d.reclassifiedCount(),
                d.recommendedLotOrder());
    }
}
