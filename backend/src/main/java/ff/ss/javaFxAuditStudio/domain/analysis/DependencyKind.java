package ff.ss.javaFxAuditStudio.domain.analysis;

/**
 * Type de dependance detectee autour d'un controller.
 */
public enum DependencyKind {
    DIRECT_CONTROLLER,
    SHARED_SERVICE,
    INHERITANCE,
    DYNAMIC_UI_BINDING,
    DYNAMIC_UI_LISTENER,
    DYNAMIC_UI_VISIBILITY,
    DYNAMIC_UI_EVENT_HANDLER
}
