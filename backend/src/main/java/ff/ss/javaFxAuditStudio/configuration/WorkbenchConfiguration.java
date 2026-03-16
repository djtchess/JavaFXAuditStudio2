package ff.ss.javaFxAuditStudio.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ff.ss.javaFxAuditStudio.application.ports.in.GetWorkbenchOverviewUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.WorkbenchCatalogPort;
import ff.ss.javaFxAuditStudio.application.service.GetWorkbenchOverviewService;

@Configuration
public class WorkbenchConfiguration {

    @Bean
    public GetWorkbenchOverviewUseCase getWorkbenchOverviewUseCase(final WorkbenchCatalogPort workbenchCatalogPort) {
        GetWorkbenchOverviewUseCase useCase;

        useCase = new GetWorkbenchOverviewService(workbenchCatalogPort);
        return useCase;
    }
}
