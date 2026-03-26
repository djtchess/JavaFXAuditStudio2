package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.domain.rules.MethodParameter;
import ff.ss.javaFxAuditStudio.domain.rules.MethodSignature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JavaFxUiTypeFilterTest {

    // --- isJavaFxUiType ---

    @Test
    void isJavaFxUiType_actionEvent_returnsTrue() {
        assertThat(JavaFxUiTypeFilter.isJavaFxUiType("ActionEvent")).isTrue();
    }

    @Test
    void isJavaFxUiType_mouseEvent_returnsTrue() {
        assertThat(JavaFxUiTypeFilter.isJavaFxUiType("MouseEvent")).isTrue();
    }

    @Test
    void isJavaFxUiType_patientDomainType_returnsFalse() {
        assertThat(JavaFxUiTypeFilter.isJavaFxUiType("Patient")).isFalse();
    }

    @Test
    void isJavaFxUiType_fullyQualifiedActionEvent_returnsTrue() {
        assertThat(JavaFxUiTypeFilter.isJavaFxUiType("javafx.event.ActionEvent")).isTrue();
    }

    @Test
    void isJavaFxUiType_nullType_returnsFalse() {
        assertThat(JavaFxUiTypeFilter.isJavaFxUiType(null)).isFalse();
    }

    @Test
    void isJavaFxUiType_blankType_returnsFalse() {
        assertThat(JavaFxUiTypeFilter.isJavaFxUiType("   ")).isFalse();
    }

    @Test
    void isJavaFxUiType_uiComponents_returnsTrue() {
        assertThat(JavaFxUiTypeFilter.isJavaFxUiType("Button")).isTrue();
        assertThat(JavaFxUiTypeFilter.isJavaFxUiType("Stage")).isTrue();
        assertThat(JavaFxUiTypeFilter.isJavaFxUiType("TableView")).isTrue();
    }

    // --- filterForDomain ---

    @Test
    void filterForDomain_removesActionEventKeepsPatient() {
        MethodSignature sig = MethodSignature.of("void", List.of(
                MethodParameter.known("ActionEvent", "event"),
                MethodParameter.known("Patient", "patient")
        ));

        MethodSignature result = JavaFxUiTypeFilter.filterForDomain(sig);

        assertThat(result.parameters()).hasSize(1);
        assertThat(result.parameters().get(0).type()).isEqualTo("Patient");
        assertThat(result.parameters().get(0).name()).isEqualTo("patient");
        assertThat(result.returnType()).isEqualTo("void");
    }

    @Test
    void filterForDomain_signatureWithoutJavaFx_returnsSameInstance() {
        MethodSignature sig = MethodSignature.of("void", List.of(
                MethodParameter.known("Patient", "patient"),
                MethodParameter.known("Long", "id")
        ));

        MethodSignature result = JavaFxUiTypeFilter.filterForDomain(sig);

        // aucun filtrage : instance identique retournee
        assertThat(result).isSameAs(sig);
    }

    @Test
    void filterForDomain_nullSignature_returnsNull() {
        assertThat(JavaFxUiTypeFilter.filterForDomain(null)).isNull();
    }

    @Test
    void filterForDomain_allParamsJavaFx_returnsEmptyParams() {
        MethodSignature sig = MethodSignature.of("void", List.of(
                MethodParameter.known("ActionEvent", "event"),
                MethodParameter.known("MouseEvent", "mouse")
        ));

        MethodSignature result = JavaFxUiTypeFilter.filterForDomain(sig);

        assertThat(result.parameters()).isEmpty();
    }
}
