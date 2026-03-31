package ff.ss.javaFxAuditStudio.domain.workbench;

public enum AnalysisStatus {
    CREATED,
    PENDING,
    IN_PROGRESS,
    RUNNING,
    INGESTING,
    CARTOGRAPHING,
    CLASSIFYING,
    PLANNING,
    GENERATING,
    REPORTING,
    COMPLETED,
    FAILED,
    /** Analyse verrouillee : aucune modification ni reclassification n'est autorisee. */
    LOCKED;

    public boolean isPipelineActive() {
        boolean active;

        active = switch (this) {
            case IN_PROGRESS, RUNNING, INGESTING, CARTOGRAPHING, CLASSIFYING, PLANNING, GENERATING, REPORTING -> true;
            default -> false;
        };
        return active;
    }

    public boolean isPipelineStep() {
        boolean pipelineStep;

        pipelineStep = switch (this) {
            case INGESTING, CARTOGRAPHING, CLASSIFYING, PLANNING, GENERATING, REPORTING -> true;
            default -> false;
        };
        return pipelineStep;
    }

    public boolean isTerminal() {
        boolean terminal;

        terminal = switch (this) {
            case COMPLETED, FAILED, LOCKED -> true;
            default -> false;
        };
        return terminal;
    }
}
