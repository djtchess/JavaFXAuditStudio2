package ff.ss.javaFxAuditStudio.domain.analysis;

import java.util.Objects;

/**
 * Dependance detectee dans le code d'un controller.
 */
public record ControllerDependency(
        DependencyKind kind,
        String target,
        String via) {

    public ControllerDependency {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(via, "via must not be null");
    }
}
