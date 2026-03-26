package ff.ss.javaFxAuditStudio.domain.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de l'enum TaskType (IAP-2).
 */
class TaskTypeTest {

    @ParameterizedTest
    @CsvSource({
        "NAMING, NAMING",
        "naming, NAMING",
        "Naming, NAMING",
        "DESCRIPTION, DESCRIPTION",
        "CLASSIFICATION_HINT, CLASSIFICATION_HINT",
        "ARTIFACT_REVIEW, ARTIFACT_REVIEW",
        "SPRING_BOOT_GENERATION, SPRING_BOOT_GENERATION"
    })
    void fromString_should_be_case_insensitive(final String input, final String expected) {
        assertThat(TaskType.fromString(input)).isEqualTo(TaskType.valueOf(expected));
    }

    @Test
    void fromString_should_throw_for_null() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> TaskType.fromString(null));
    }

    @Test
    void fromString_should_throw_for_blank() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> TaskType.fromString("   "));
    }

    @Test
    void fromString_should_throw_for_unknown_value() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> TaskType.fromString("UNKNOWN_TASK"));
    }

    @Test
    void name_should_return_enum_constant_name() {
        assertThat(TaskType.NAMING.name()).isEqualTo("NAMING");
        assertThat(TaskType.SPRING_BOOT_GENERATION.name()).isEqualTo("SPRING_BOOT_GENERATION");
    }
}
