package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.GetWorkbenchOverviewUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.WorkbenchCatalogPort;
import ff.ss.javaFxAuditStudio.domain.workbench.WorkbenchOverview;

public final class GetWorkbenchOverviewService implements GetWorkbenchOverviewUseCase {

    private final WorkbenchCatalogPort workbenchCatalogPort;

    public GetWorkbenchOverviewService(final WorkbenchCatalogPort workbenchCatalogPort) {
        this.workbenchCatalogPort = workbenchCatalogPort;
    }

    @Override
    public WorkbenchOverview handle() {
        WorkbenchOverview workbenchOverview;

        workbenchOverview = workbenchCatalogPort.load();
        return workbenchOverview;
    }
}
