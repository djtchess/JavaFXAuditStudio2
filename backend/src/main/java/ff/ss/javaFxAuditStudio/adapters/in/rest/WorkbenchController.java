package ff.ss.javaFxAuditStudio.adapters.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.WorkbenchOverviewResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.mapper.WorkbenchOverviewResponseMapper;
import ff.ss.javaFxAuditStudio.application.ports.in.GetWorkbenchOverviewUseCase;

@Tag(name = "Workbench")
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

    @Operation(summary = "Vue globale du workbench", description = "Retourne la vue d'ensemble du workbench : produit, lots de refactoring planifies et agents disponibles.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Overview disponible")
    })
    @GetMapping("/overview")
    public WorkbenchOverviewResponse getOverview() {
        WorkbenchOverviewResponse response;

        response = workbenchOverviewResponseMapper.toResponse(getWorkbenchOverviewUseCase.handle());
        return response;
    }
}
