package ff.ss.javaFxAuditStudio.domain.migration;

import java.util.Objects;

public record RegressionRisk(
        String description,
        RiskLevel level,
        String mitigation
) {
    public RegressionRisk {
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(mitigation, "mitigation must not be null");
    }
}
