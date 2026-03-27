package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.analysis.ControllerFlowAnalysis;
import ff.ss.javaFxAuditStudio.domain.analysis.ProjectDependencyGraph;
import ff.ss.javaFxAuditStudio.domain.analysis.ProjectDeltaAnalysis;

import java.util.List;

public interface AdvancedAnalysisUseCase {

    ControllerFlowAnalysis analyzeControllerFlow(String sessionId);

    ProjectDependencyGraph analyzeProjectDependencies(String projectId, List<String> controllerRefs);

    ProjectDeltaAnalysis analyzeProjectDelta(
            String projectId,
            List<String> baselineControllerRefs,
            List<String> currentControllerRefs);
}
