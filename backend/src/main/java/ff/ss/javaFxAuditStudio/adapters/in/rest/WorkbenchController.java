package ff.ss.javaFxAuditStudio.adapters.in.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.WorkbenchOverviewResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.mapper.WorkbenchOverviewResponseMapper;
import ff.ss.javaFxAuditStudio.application.ports.in.GetWorkbenchOverviewUseCase;

@RestController
@RequestMapping("/api/v1/workbench")
public class WorkbenchController {

    private final GetWorkbenchOverviewUseCase getWorkbenchOverviewUseCase;
    private final WorkbenchOverviewResponseMapper workbenchOverviewResponseMapper;

    public WorkbenchController(
            final GetWorkbenchOverviewUseCase getWorkbenchOverviewUseCase,
            final WorkbenchOverviewResponseMapper workbenchOverviewResponseMapper) {
        this.getWorkbenchOverviewUseCase = getWorkbenchOverviewUseCase;
        this.workbenchOverviewResponseMapper = workbenchOverviewResponseMapper;
    }

    @GetMapping("/overview")
    public WorkbenchOverviewResponse getOverview() {
        WorkbenchOverviewResponse response;

        response = workbenchOverviewResponseMapper.toResponse(getWorkbenchOverviewUseCase.handle());
        return response;
    }
}
