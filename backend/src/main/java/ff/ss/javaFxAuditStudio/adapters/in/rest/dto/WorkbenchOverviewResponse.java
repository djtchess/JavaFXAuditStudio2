package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.List;
import java.util.Objects;

public record WorkbenchOverviewResponse(
        String productName,
        String summary,
        String frontendTarget,
        String backendTarget,
        List<RefactoringLotResponse> lots,
        List<AgentOverviewResponse> agents) {

    public WorkbenchOverviewResponse {
        Objects.requireNonNull(productName, "productName est obligatoire");
        Objects.requireNonNull(summary, "summary est obligatoire");
        Objects.requireNonNull(frontendTarget, "frontendTarget est obligatoire");
        Objects.requireNonNull(backendTarget, "backendTarget est obligatoire");
        lots = (lots != null) ? List.copyOf(lots) : List.of();
        agents = (agents != null) ? List.copyOf(agents) : List.of();
    }
}
