package ff.ss.javaFxAuditStudio.domain.workbench;

import java.util.List;

public record WorkbenchOverview(
        String productName,
        String summary,
        String frontendTarget,
        String backendTarget,
        List<RefactoringLot> lots,
        List<AgentOverview> agents) {
}
