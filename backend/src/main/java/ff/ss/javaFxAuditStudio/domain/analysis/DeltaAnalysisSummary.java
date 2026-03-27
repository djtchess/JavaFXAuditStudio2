package ff.ss.javaFxAuditStudio.domain.analysis;

/**
 * Resume differentiel entre une classification en cache et l'etat courant du source.
 */
public record DeltaAnalysisSummary(
        int addedRules,
        int removedRules,
        int changedRules) {

    public DeltaAnalysisSummary {
        if (addedRules < 0 || removedRules < 0 || changedRules < 0) {
            throw new IllegalArgumentException("delta counters must be >= 0");
        }
    }

    public static DeltaAnalysisSummary none() {
        return new DeltaAnalysisSummary(0, 0, 0);
    }

    public boolean hasChanges() {
        return addedRules > 0 || removedRules > 0 || changedRules > 0;
    }
}
