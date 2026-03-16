package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.workbench.WorkbenchOverview;

public interface WorkbenchCatalogPort {

    WorkbenchOverview load();
}
