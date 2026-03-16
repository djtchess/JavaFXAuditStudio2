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
    void shouldAlwaysReturn5Lots() {
        List<PlannedLot> lots = adapter.plan("SampleController");

        assertThat(lots).hasSize(5);
    }

    @Test
    void shouldHaveLotNumbersFrom1To5() {
        List<PlannedLot> lots = adapter.plan("SampleController");

        List<Integer> lotNumbers = lots.stream().map(PlannedLot::lotNumber).toList();
        assertThat(lotNumbers).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void shouldHaveNonEmptyObjectives() {
        List<PlannedLot> lots = adapter.plan("SampleController");

        assertThat(lots).allSatisfy(lot -> assertThat(lot.objective()).isNotBlank());
    }

    @Test
    void shouldHandleNullControllerRef() {
        assertThatNoException().isThrownBy(() -> {
            List<PlannedLot> lots = adapter.plan(null);
            assertThat(lots).hasSize(5);
        });
    }
}
