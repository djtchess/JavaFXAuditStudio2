package ff.ss.javaFxAuditStudio.adapters.in.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AgentOverviewResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.RefactoringLotResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.WorkbenchOverviewResponse;
import ff.ss.javaFxAuditStudio.domain.workbench.AgentOverview;
import ff.ss.javaFxAuditStudio.domain.workbench.RefactoringLot;
import ff.ss.javaFxAuditStudio.domain.workbench.WorkbenchOverview;

@Component
public class WorkbenchOverviewResponseMapper {

    public WorkbenchOverviewResponse toResponse(final WorkbenchOverview workbenchOverview) {
        WorkbenchOverviewResponse response;

        response = new WorkbenchOverviewResponse(
                workbenchOverview.productName(),
                workbenchOverview.summary(),
                workbenchOverview.frontendTarget(),
                workbenchOverview.backendTarget(),
                mapLots(workbenchOverview.lots()),
                mapAgents(workbenchOverview.agents()));
        return response;
    }

    private List<RefactoringLotResponse> mapLots(final List<RefactoringLot> lots) {
        List<RefactoringLotResponse> responses;

        responses = lots.stream()
                .map(lot -> new RefactoringLotResponse(
                        lot.number(),
                        lot.title(),
                        lot.objective(),
                        lot.primaryOutcome()))
                .toList();
        return responses;
    }

    private List<AgentOverviewResponse> mapAgents(final List<AgentOverview> agents) {
        List<AgentOverviewResponse> responses;

        responses = agents.stream()
                .map(agent -> new AgentOverviewResponse(
                        agent.id(),
                        agent.label(),
                        agent.responsibility(),
                        agent.preferredModel()))
                .toList();
        return responses;
    }
}
