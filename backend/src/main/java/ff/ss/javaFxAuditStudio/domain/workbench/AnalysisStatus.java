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
    /** Analyse verrouilee : aucune modification ni reclassification n'est autorisee. */
    LOCKED
}
