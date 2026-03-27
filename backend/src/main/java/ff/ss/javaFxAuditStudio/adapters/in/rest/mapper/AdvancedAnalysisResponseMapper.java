package ff.ss.javaFxAuditStudio.adapters.in.rest.mapper;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ControllerFlowResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ProjectDependencyGraphResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ProjectDeltaResponse;
import ff.ss.javaFxAuditStudio.domain.analysis.ControllerFlowAnalysis;
import ff.ss.javaFxAuditStudio.domain.analysis.ProjectDependencyGraph;
import ff.ss.javaFxAuditStudio.domain.analysis.ProjectDeltaAnalysis;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdvancedAnalysisResponseMapper {

    public ControllerFlowResponse toResponse(final ControllerFlowAnalysis flow) {
        List<ControllerFlowResponse.StateTransitionDto> transitions = flow.transitions().stream()
                .map(transition -> new ControllerFlowResponse.StateTransitionDto(
                        transition.sourceState(),
                        transition.targetState(),
                        transition.triggerMethod(),
                        transition.guardExpression(),
                        transition.sourceLine()))
                .toList();
        return new ControllerFlowResponse(
                flow.controllerRef(),
                flow.controllerName(),
                flow.stateMachineDetected(),
                flow.stateMachineConfidence(),
                flow.detectionLevel(),
                flow.states(),
                transitions,
                flow.policyGuardCandidates(),
                flow.uiGuardMethods(),
                flow.evidence(),
                flow.warnings());
    }

    public ProjectDependencyGraphResponse toResponse(final ProjectDependencyGraph graph) {
        List<ProjectDependencyGraphResponse.ControllerNodeDto> nodes = graph.controllers().stream()
                .map(node -> new ProjectDependencyGraphResponse.ControllerNodeDto(
                        node.controllerRef(),
                        node.controllerName(),
                        node.injectedServices(),
                        node.outgoingDependencies(),
                        node.incomingDependencies()))
                .toList();
        List<ProjectDependencyGraphResponse.DependencyEdgeDto> edges = graph.dependencies().stream()
                .map(edge -> new ProjectDependencyGraphResponse.DependencyEdgeDto(
                        edge.fromController(),
                        edge.toController(),
                        edge.type().name(),
                        edge.evidence()))
                .toList();
        return new ProjectDependencyGraphResponse(
                graph.projectId(),
                nodes,
                edges,
                graph.recommendedOrder(),
                graph.warnings());
    }

    public ProjectDeltaResponse toResponse(final ProjectDeltaAnalysis delta) {
        List<ProjectDeltaResponse.ControllerDeltaDto> controllerDeltas = delta.controllerDeltas().stream()
                .map(entry -> new ProjectDeltaResponse.ControllerDeltaDto(
                        entry.controllerRef(),
                        entry.status().name(),
                        entry.addedRules(),
                        entry.removedRules(),
                        entry.addedTransitions(),
                        entry.removedTransitions(),
                        entry.notes()))
                .toList();
        return new ProjectDeltaResponse(
                delta.projectId(),
                delta.baselineLabel(),
                delta.currentLabel(),
                delta.newControllers(),
                delta.removedControllers(),
                delta.modifiedControllers(),
                delta.unchangedControllers(),
                controllerDeltas,
                delta.warnings());
    }
}
