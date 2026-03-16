package ff.ss.javaFxAuditStudio.domain.cartography;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ControllerCartographyTest {

    @Test
    void hasUnknowns_returnsFalse_whenNoUnknowns() {
        ControllerCartography cartography;

        cartography = new ControllerCartography(
                "com/example/MainController.java",
                "main.fxml",
                List.of(),
                List.of(),
                List.of());

        assertThat(cartography.hasUnknowns()).isFalse();
    }

    @Test
    void hasUnknowns_returnsTrue_whenUnknownsPresent() {
        CartographyUnknown unknown;
        ControllerCartography cartography;

        unknown = new CartographyUnknown("line 42", "Dynamic binding unresolvable at static analysis time");
        cartography = new ControllerCartography(
                "com/example/MainController.java",
                "main.fxml",
                List.of(),
                List.of(),
                List.of(unknown));

        assertThat(cartography.hasUnknowns()).isTrue();
    }

    @Test
    void constructor_definesImmutableLists() {
        List<CartographyUnknown> mutableUnknowns;
        CartographyUnknown unknown;
        ControllerCartography cartography;

        mutableUnknowns = new ArrayList<>();
        unknown = new CartographyUnknown("line 10", "Cannot resolve fx:id reference");
        mutableUnknowns.add(unknown);

        cartography = new ControllerCartography(
                "com/example/MainController.java",
                "main.fxml",
                List.of(),
                List.of(),
                mutableUnknowns);

        mutableUnknowns.add(new CartographyUnknown("line 20", "Another unknown"));

        assertThat(cartography.unknowns()).hasSize(1);
    }
}
