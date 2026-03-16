package ff.ss.javaFxAuditStudio.domain.migration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MigrationPlan(
        String controllerRef,
        List<PlannedLot> lots,
        boolean compilable
) {
    public MigrationPlan {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        if (lots != null) {
            final List<PlannedLot> sorted = new ArrayList<>(lots);
            sorted.sort(Comparator.comparingInt(PlannedLot::lotNumber));
            lots = List.copyOf(sorted);
        } else {
            lots = List.of();
        }
    }

    public Optional<PlannedLot> lotByNumber(final int number) {
        return lots.stream()
                .filter(lot -> lot.lotNumber() == number)
                .findFirst();
    }
}
