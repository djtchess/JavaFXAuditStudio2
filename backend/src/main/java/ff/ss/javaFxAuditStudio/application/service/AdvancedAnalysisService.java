package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.AdvancedAnalysisUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceReaderPort;
import ff.ss.javaFxAuditStudio.configuration.AnalysisProperties;
import ff.ss.javaFxAuditStudio.domain.analysis.ControllerFlowAnalysis;
import ff.ss.javaFxAuditStudio.domain.analysis.ProjectDependencyGraph;
import ff.ss.javaFxAuditStudio.domain.analysis.ProjectDeltaAnalysis;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AdvancedAnalysisService implements AdvancedAnalysisUseCase {

    private final AnalysisSessionPort analysisSessionPort;
    private final SourceReaderPort sourceReaderPort;
    private final RuleExtractionPort ruleExtractionPort;
    private final AnalysisProperties.ClassificationPatterns classificationPatterns;

    public AdvancedAnalysisService(
            final AnalysisSessionPort analysisSessionPort,
            final SourceReaderPort sourceReaderPort,
            final RuleExtractionPort ruleExtractionPort,
            final AnalysisProperties analysisProperties) {
        this.analysisSessionPort = Objects.requireNonNull(analysisSessionPort, "analysisSessionPort must not be null");
        this.sourceReaderPort = Objects.requireNonNull(sourceReaderPort, "sourceReaderPort must not be null");
        this.ruleExtractionPort = Objects.requireNonNull(ruleExtractionPort, "ruleExtractionPort must not be null");
        this.classificationPatterns = analysisProperties != null
                && analysisProperties.classificationPatterns() != null
                ? analysisProperties.classificationPatterns()
                : new AnalysisProperties.ClassificationPatterns(
                        null, null, null, null, null, null, null, null);
    }

    @Override
    public ControllerFlowAnalysis analyzeControllerFlow(final String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        AnalysisSession session = analysisSessionPort.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
        String source = ControllerAnalysisSupport.readSource(session.controllerName(), sourceReaderPort);
        return ControllerAnalysisSupport.analyzeFlow(session.controllerName(), source, classificationPatterns);
    }

    @Override
    public ProjectDependencyGraph analyzeProjectDependencies(
            final String projectId,
            final List<String> controllerRefs) {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(controllerRefs, "controllerRefs must not be null");
        Map<String, ControllerAnalysisSupport.ControllerSnapshot> snapshots =
                ControllerAnalysisSupport.buildSnapshots(controllerRefs, sourceReaderPort, ruleExtractionPort, classificationPatterns);
        List<ProjectDependencyGraph.DependencyEdge> edges = ControllerAnalysisSupport.buildEdges(snapshots);
        Map<String, int[]> counts = dependencyCounts(snapshots, edges);
        List<ProjectDependencyGraph.ControllerNode> nodes = buildNodes(snapshots, counts);
        List<String> order = ControllerAnalysisSupport.topologicalOrder(snapshots, edges);
        List<String> warnings = new ArrayList<>();
        if (edges.isEmpty()) {
            warnings.add("Aucune dependance inter-controller detectee");
        }
        return new ProjectDependencyGraph(projectId, nodes, edges, order, warnings);
    }

    @Override
    public ProjectDeltaAnalysis analyzeProjectDelta(
            final String projectId,
            final List<String> baselineControllerRefs,
            final List<String> currentControllerRefs) {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(baselineControllerRefs, "baselineControllerRefs must not be null");
        Objects.requireNonNull(currentControllerRefs, "currentControllerRefs must not be null");
        Map<String, ControllerAnalysisSupport.ControllerSnapshot> baseline =
                ControllerAnalysisSupport.buildSnapshots(baselineControllerRefs, sourceReaderPort, ruleExtractionPort, classificationPatterns);
        Map<String, ControllerAnalysisSupport.ControllerSnapshot> current =
                ControllerAnalysisSupport.buildSnapshots(currentControllerRefs, sourceReaderPort, ruleExtractionPort, classificationPatterns);
        List<ProjectDeltaAnalysis.ControllerDelta> deltas = ControllerAnalysisSupport.diffSnapshots(baseline, current);
        int newControllers = countStatus(deltas, ProjectDeltaAnalysis.DeltaStatus.NEW);
        int removedControllers = countStatus(deltas, ProjectDeltaAnalysis.DeltaStatus.REMOVED);
        int modifiedControllers = countStatus(deltas, ProjectDeltaAnalysis.DeltaStatus.MODIFIED);
        int unchangedControllers = countStatus(deltas, ProjectDeltaAnalysis.DeltaStatus.UNCHANGED);
        return new ProjectDeltaAnalysis(
                projectId,
                joinLabel(baselineControllerRefs),
                joinLabel(currentControllerRefs),
                newControllers,
                removedControllers,
                modifiedControllers,
                unchangedControllers,
                deltas,
                List.of());
    }

    private Map<String, int[]> dependencyCounts(
            final Map<String, ControllerAnalysisSupport.ControllerSnapshot> snapshots,
            final List<ProjectDependencyGraph.DependencyEdge> edges) {
        Map<String, int[]> counts = new LinkedHashMap<>();
        snapshots.keySet().forEach(ref -> counts.put(ref, new int[] {0, 0}));
        for (ProjectDependencyGraph.DependencyEdge edge : edges) {
            int[] from = counts.get(edge.fromController());
            int[] to = counts.get(edge.toController());
            if (from != null) {
                from[0]++;
            }
            if (to != null) {
                to[1]++;
            }
        }
        return counts;
    }

    private List<ProjectDependencyGraph.ControllerNode> buildNodes(
            final Map<String, ControllerAnalysisSupport.ControllerSnapshot> snapshots,
            final Map<String, int[]> counts) {
        List<ProjectDependencyGraph.ControllerNode> nodes = new ArrayList<>();
        for (ControllerAnalysisSupport.ControllerSnapshot snapshot : snapshots.values()) {
            int[] current = counts.getOrDefault(snapshot.controllerRef(), new int[] {0, 0});
            nodes.add(new ProjectDependencyGraph.ControllerNode(
                    snapshot.controllerRef(),
                    snapshot.controllerName(),
                    new ArrayList<>(snapshot.injectedServices()),
                    current[0],
                    current[1]));
        }
        return List.copyOf(nodes);
    }

    private int countStatus(
            final List<ProjectDeltaAnalysis.ControllerDelta> deltas,
            final ProjectDeltaAnalysis.DeltaStatus status) {
        int count = 0;
        for (ProjectDeltaAnalysis.ControllerDelta delta : deltas) {
            if (delta.status() == status) {
                count++;
            }
        }
        return count;
    }

    private String joinLabel(final List<String> refs) {
        if (refs.isEmpty()) {
            return "empty";
        }
        return String.join(",", refs);
    }
}
