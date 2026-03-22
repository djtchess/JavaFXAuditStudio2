package ff.ss.javaFxAuditStudio.domain.workbench;

public enum AnalysisStatus {
    CREATED,
    PENDING,
    IN_PROGRESS,
    RUNNING,
    COMPLETED,
    FAILED,
    /** Analyse verrouilee : aucune modification ni reclassification n'est autorisee. */
    LOCKED
}
