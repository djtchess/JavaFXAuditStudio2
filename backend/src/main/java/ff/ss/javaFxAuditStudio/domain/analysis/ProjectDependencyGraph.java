package ff.ss.javaFxAuditStudio.domain.analysis;

import java.util.List;
import java.util.Objects;

public record ProjectDependencyGraph(
        String projectId,
        List<ControllerNode> controllers,
        List<DependencyEdge> dependencies,
        List<String> recommendedOrder,
        List<String> warnings) {

    public ProjectDependencyGraph {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(controllers, "controllers must not be null");
        Objects.requireNonNull(dependencies, "dependencies must not be null");
        Objects.requireNonNull(recommendedOrder, "recommendedOrder must not be null");
        Objects.requireNonNull(warnings, "warnings must not be null");
        controllers = List.copyOf(controllers);
        dependencies = List.copyOf(dependencies);
        recommendedOrder = List.copyOf(recommendedOrder);
        warnings = List.copyOf(warnings);
    }

    public record ControllerNode(
            String controllerRef,
            String controllerName,
            List<String> injectedServices,
            int outgoingDependencies,
            int incomingDependencies) {

        public ControllerNode {
            Objects.requireNonNull(controllerRef, "controllerRef must not be null");
            Objects.requireNonNull(controllerName, "controllerName must not be null");
            Objects.requireNonNull(injectedServices, "injectedServices must not be null");
            if (outgoingDependencies < 0 || incomingDependencies < 0) {
                throw new IllegalArgumentException("dependency counts must be >= 0");
            }
            injectedServices = List.copyOf(injectedServices);
        }
    }

    public record DependencyEdge(
            String fromController,
            String toController,
            DependencyType type,
            String evidence) {

        public DependencyEdge {
            Objects.requireNonNull(fromController, "fromController must not be null");
            Objects.requireNonNull(toController, "toController must not be null");
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(evidence, "evidence must not be null");
        }
    }

    public enum DependencyType {
        SHARED_SERVICE,
        DIRECT_CALL
    }
}
