package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionResult;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAS-020 — Tests de detection des methodes garde booléennes par regex
 * dans JavaControllerRuleExtractionAdapter.
 */
class JavaControllerRuleExtractionAdapterGuardTest {

    private JavaControllerRuleExtractionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JavaControllerRuleExtractionAdapter();
    }

    @Test
    void isFormValid_doitEtreClassifieBusinessPolicy() {
        String source = """
                public class FormController {
                    private boolean isFormValid() {
                        return nameField != null && !nameField.getText().isBlank();
                    }
                }
                """;

        ExtractionResult result = adapter.extract("FormController.java", source);

        List<BusinessRule> gardes = result.rules().stream()
                .filter(r -> r.description().contains("isFormValid"))
                .toList();
        assertThat(gardes).isNotEmpty();
        assertThat(gardes.get(0).responsibilityClass()).isEqualTo(ResponsibilityClass.BUSINESS);
        assertThat(gardes.get(0).extractionCandidate()).isEqualTo(ExtractionCandidate.POLICY);
    }

    @Test
    void canSave_avecParametre_doitEtreExtraite() {
        String source = """
                public class PatientController {
                    public boolean canSave(Patient p) {
                        return p != null && p.isValid();
                    }
                }
                """;

        ExtractionResult result = adapter.extract("PatientController.java", source);

        List<BusinessRule> gardes = result.rules().stream()
                .filter(r -> r.description().contains("canSave"))
                .toList();
        assertThat(gardes).isNotEmpty();
        assertThat(gardes.get(0).responsibilityClass()).isEqualTo(ResponsibilityClass.BUSINESS);
        assertThat(gardes.get(0).extractionCandidate()).isEqualTo(ExtractionCandidate.POLICY);
    }

    @Test
    void sourceSansGarde_aucuneRegleBusiness() {
        String source = """
                public class SimpleController {
                    @FXML
                    public void handleClick() {
                        label.setText("clicked");
                    }
                }
                """;

        ExtractionResult result = adapter.extract("SimpleController.java", source);

        List<BusinessRule> businessGardes = result.rules().stream()
                .filter(r -> r.responsibilityClass() == ResponsibilityClass.BUSINESS
                        && r.description().contains("Methode garde"))
                .toList();
        assertThat(businessGardes).isEmpty();
    }

    @Test
    void hasAdminPermission_doitEtreClassifieBusinessPolicy() {
        String source = """
                public class AdminController {
                    private boolean hasAdminPermission() {
                        return userSession.hasRole("ADMIN");
                    }
                }
                """;

        ExtractionResult result = adapter.extract("AdminController.java", source);

        List<BusinessRule> gardes = result.rules().stream()
                .filter(r -> r.description().contains("hasAdminPermission"))
                .toList();
        assertThat(gardes).isNotEmpty();
        assertThat(gardes.get(0).responsibilityClass()).isEqualTo(ResponsibilityClass.BUSINESS);
        assertThat(gardes.get(0).extractionCandidate()).isEqualTo(ExtractionCandidate.POLICY);
    }

    @Test
    void isDisable_doitEtreExclueDesPolicies() {
        String source = """
                public class UiController {
                    private boolean isDisable() {
                        return button == null;
                    }
                }
                """;

        ExtractionResult result = adapter.extract("UiController.java", source);

        boolean policyGuard = result.rules().stream()
                .anyMatch(r -> r.description().contains("isDisable")
                        && r.extractionCandidate() == ExtractionCandidate.POLICY);
        assertThat(policyGuard).isFalse();
    }
}
