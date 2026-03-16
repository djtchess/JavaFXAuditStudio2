package ff.ss.javaFxAuditStudio.domain.migration;

import java.util.List;
import java.util.Objects;

public record PlannedLot(
        int lotNumber,
        String title,
        String objective,
        List<String> extractionCandidates,
        List<RegressionRisk> risks
) {
    public PlannedLot {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(objective, "objective must not be null");
        if (lotNumber < 1 || lotNumber > 5) {
            throw new IllegalArgumentException("lotNumber must be between 1 and 5, got: " + lotNumber);
        }
        extractionCandidates = (extractionCandidates != null) ? List.copyOf(extractionCandidates) : List.of();
        risks = (risks != null) ? List.copyOf(risks) : List.of();
    }
}
