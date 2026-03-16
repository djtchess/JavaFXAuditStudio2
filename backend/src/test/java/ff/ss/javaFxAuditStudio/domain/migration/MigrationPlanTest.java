package ff.ss.javaFxAuditStudio.domain.migration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MigrationPlanTest {

    private static PlannedLot lotWithNumber(final int lotNumber) {
        return new PlannedLot(
                lotNumber,
                "Lot " + lotNumber,
                "Objectif du lot " + lotNumber,
                List.of(),
                List.of());
    }

    @Test
    void lots_areSortedByLotNumber() {
        PlannedLot lot3;
        PlannedLot lot1;
        PlannedLot lot2;
        MigrationPlan plan;

        lot3 = lotWithNumber(3);
        lot1 = lotWithNumber(1);
        lot2 = lotWithNumber(2);
        plan = new MigrationPlan("com/example/MyController.java", List.of(lot3, lot1, lot2), true);

        assertThat(plan.lots()).extracting(PlannedLot::lotNumber)
                .containsExactly(1, 2, 3);
    }

    @Test
    void compilable_isFalse_whenLotsEmpty() {
        MigrationPlan plan;

        plan = new MigrationPlan("com/example/MyController.java", List.of(), false);

        assertThat(plan.compilable()).isFalse();
        assertThat(plan.lots()).isEmpty();
    }

    @Test
    void lotByNumber_returnsEmpty_whenNotFound() {
        MigrationPlan plan;
        Optional<PlannedLot> found;

        plan = new MigrationPlan("com/example/MyController.java", List.of(lotWithNumber(1)), true);

        found = plan.lotByNumber(5);

        assertThat(found).isEmpty();
    }

    @Test
    void lotByNumber_returnsLot_whenFound() {
        PlannedLot lot2;
        MigrationPlan plan;
        Optional<PlannedLot> found;

        lot2 = lotWithNumber(2);
        plan = new MigrationPlan("com/example/MyController.java", List.of(lotWithNumber(1), lot2), true);

        found = plan.lotByNumber(2);

        assertThat(found).isPresent();
        assertThat(found.get().lotNumber()).isEqualTo(2);
    }

    @Test
    void plannedLot_throwsIllegalArgumentException_whenLotNumberOutOfRange() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> lotWithNumber(0))
                .withMessageContaining("lotNumber");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> lotWithNumber(6))
                .withMessageContaining("lotNumber");
    }
}
