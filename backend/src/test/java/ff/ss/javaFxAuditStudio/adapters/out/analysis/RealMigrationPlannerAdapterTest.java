package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.domain.migration.PlannedLot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class RealMigrationPlannerAdapterTest {

    private RealMigrationPlannerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RealMigrationPlannerAdapter();
    }

    @Test
    void shouldReturn3LotsForLowComplexity() {
        List<PlannedLot> lots = adapter.plan("SampleController", 2, 3, 5);

        assertThat(lots).hasSize(3);
    }

    @Test
    void shouldReturn4LotsForMediumComplexity() {
        List<PlannedLot> lots = adapter.plan("SampleController", 5, 10, 20);

        assertThat(lots).hasSize(4);
    }

    @Test
    void shouldReturn5LotsForHighComplexity() {
        List<PlannedLot> lots = adapter.plan("SampleController", 10, 20, 40);

        assertThat(lots).hasSize(5);
        List<Integer> lotNumbers = lots.stream().map(PlannedLot::lotNumber).toList();
        assertThat(lotNumbers).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void shouldHaveNonEmptyObjectives() {
        List<PlannedLot> lots = adapter.plan("SampleController", 5, 10, 25);

        assertThat(lots).allSatisfy(lot -> assertThat(lot.objective()).isNotBlank());
    }

    @Test
    void shouldHandleNullControllerRef() {
        assertThatNoException().isThrownBy(() -> {
            List<PlannedLot> lots = adapter.plan(null, 0, 0, 0);
            assertThat(lots).hasSize(3);
        });
    }

    @Test
    void shouldReturnHighRiskForHighComplexity() {
        List<PlannedLot> lots = adapter.plan("SampleController", 10, 20, 25);

        assertThat(lots).allSatisfy(lot ->
                assertThat(lot.risks()).isNotEmpty()
        );
    }

    @Test
    void shouldReturnDefaultLotsViaBackwardCompatMethod() {
        List<PlannedLot> lots = adapter.plan("SampleController");

        assertThat(lots).hasSize(3);
    }
}
