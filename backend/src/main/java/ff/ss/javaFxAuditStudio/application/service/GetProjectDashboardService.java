package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.GetProjectDashboardUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ProjectDashboardPort;
import ff.ss.javaFxAuditStudio.domain.workbench.ProjectDashboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Optional;

/**
 * Service applicatif pour le cas d'usage JAS-015 — tableau de bord projet.
 * Assemble via {@code ProjectDashboardConfiguration} ; pas de @Service.
 */
public class GetProjectDashboardService implements GetProjectDashboardUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetProjectDashboardService.class);

    private final ProjectDashboardPort port;

    public GetProjectDashboardService(final ProjectDashboardPort port) {
        this.port = port;
    }

    @Override
    @Cacheable(value = "project-dashboard", key = "#projectId")
    public Optional<ProjectDashboard> get(final String projectId) {
        log.debug("[dashboard] calcul tableau de bord pour projectId={}", projectId);
        Optional<ProjectDashboard> result = port.computeDashboard(projectId);
        log.debug("[dashboard] resultat present={} pour projectId={}", result.isPresent(), projectId);
        return result;
    }

    @Override
    public List<String> listProjects() {
        log.debug("[dashboard] listProjects appelee");
        return port.findAllProjectIds();
    }
}
