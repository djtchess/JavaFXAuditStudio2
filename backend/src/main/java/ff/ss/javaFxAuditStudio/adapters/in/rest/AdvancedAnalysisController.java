package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ControllerFlowResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ProjectDependenciesRequest;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ProjectDependencyGraphResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ProjectDeltaRequest;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ProjectDeltaResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.mapper.AdvancedAnalysisResponseMapper;
import ff.ss.javaFxAuditStudio.application.ports.in.AdvancedAnalysisUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analyse avancee", description = "Flow, dependances inter-controllers et delta analysis")
@RestController
@RequestMapping("/api/v1")
public class AdvancedAnalysisController {

    private final AdvancedAnalysisUseCase advancedAnalysisUseCase;
    private final AdvancedAnalysisResponseMapper responseMapper;

    public AdvancedAnalysisController(
            final AdvancedAnalysisUseCase advancedAnalysisUseCase,
            final AdvancedAnalysisResponseMapper responseMapper) {
        this.advancedAnalysisUseCase = advancedAnalysisUseCase;
        this.responseMapper = responseMapper;
    }

    @Operation(summary = "Analyser le flow d'un controller", description = "Retourne la state machine detectee, les gardes policy et les gardes purement UI.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analyse disponible"),
        @ApiResponse(responseCode = "404", description = "Session introuvable")
    })
    @GetMapping("/analysis/sessions/{sessionId}/flow")
    public ResponseEntity<ControllerFlowResponse> getControllerFlow(
            @Parameter(name = "sessionId", description = "Identifiant de la session", required = true)
            @PathVariable final String sessionId) {
        return ResponseEntity.ok(responseMapper.toResponse(
                advancedAnalysisUseCase.analyzeControllerFlow(sessionId)));
    }

    @Operation(summary = "Analyser les dependances inter-controllers", description = "Construit un graphe de dependances a partir d'une liste de controllers.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Graphe disponible")
    })
    @PostMapping("/projects/analysis/dependencies")
    public ResponseEntity<ProjectDependencyGraphResponse> getDependencies(
            @RequestBody final ProjectDependenciesRequest request) {
        return ResponseEntity.ok(responseMapper.toResponse(
                advancedAnalysisUseCase.analyzeProjectDependencies(
                        request.projectId(),
                        request.controllerRefs())));
    }

    @Operation(summary = "Analyser le delta entre deux lots de controllers", description = "Compare un lot baseline a un lot courant pour produire un delta exploitable.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Delta disponible")
    })
    @PostMapping("/projects/analysis/delta")
    public ResponseEntity<ProjectDeltaResponse> getDelta(
            @RequestBody final ProjectDeltaRequest request) {
        return ResponseEntity.ok(responseMapper.toResponse(
                advancedAnalysisUseCase.analyzeProjectDelta(
                        request.projectId(),
                        request.baselineControllerRefs(),
                        request.currentControllerRefs())));
    }
}
