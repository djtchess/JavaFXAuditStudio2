package ff.ss.javaFxAuditStudio.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import ff.ss.javaFxAuditStudio.adapters.out.persistence.AnalysisSessionRepository;
import ff.ss.javaFxAuditStudio.adapters.out.persistence.BusinessRuleRepository;
import ff.ss.javaFxAuditStudio.adapters.out.persistence.JpaProjectDashboardAdapter;
import ff.ss.javaFxAuditStudio.adapters.out.persistence.RuleClassificationAuditRepository;
import ff.ss.javaFxAuditStudio.application.ports.in.GetProjectDashboardUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ProjectDashboardPort;
import ff.ss.javaFxAuditStudio.application.service.GetProjectDashboardService;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration Spring pour JAS-015 — tableau de bord projet.
 * Active le cache Caffeine avec TTL 60s sur "project-dashboard".
 */
@Configuration
@EnableCaching
public class ProjectDashboardConfiguration {

    @Bean
    public ProjectDashboardPort projectDashboardPort(
            final AnalysisSessionRepository sessionRepository,
            final BusinessRuleRepository businessRuleRepository,
            final RuleClassificationAuditRepository auditRepository) {
        return new JpaProjectDashboardAdapter(sessionRepository, businessRuleRepository, auditRepository);
    }

    @Bean
    public GetProjectDashboardUseCase getProjectDashboardUseCase(
            final ProjectDashboardPort projectDashboardPort) {
        return new GetProjectDashboardService(projectDashboardPort);
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("project-dashboard");
        manager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS));
        return manager;
    }
}
