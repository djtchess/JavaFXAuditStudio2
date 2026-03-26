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
 * JAS-020 — Tests de detection des methodes garde booléennes par JavaParserRuleExtractionAdapter.
 * Verifie que les methodes isXxx, canXxx, hasXxx, shouldXxx sont classifiees BUSINESS / POLICY.
 */
class JavaParserRuleExtractionAdapterGuardTest {

    private JavaParserRuleExtractionAdapter adapter;

    @BeforeEach
    void setUp() {
        JavaControllerRuleExtractionAdapter fallback = new JavaControllerRuleExtractionAdapter();
        adapter = new JavaParserRuleExtractionAdapter(fallback);
    }

    @Test
    void isFormValid_doitEtreClassifieBusiness() {
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
    void canSavePatient_doitEtreClassifieBusiness() {
        String source = """
                public class PatientController {
                    public boolean canSavePatient() {
                        return patient != null && patient.isValid();
                    }
                }
                """;

        ExtractionResult result = adapter.extract("PatientController.java", source);

        List<BusinessRule> gardes = result.rules().stream()
                .filter(r -> r.description().contains("canSavePatient"))
                .toList();
        assertThat(gardes).isNotEmpty();
        assertThat(gardes.get(0).responsibilityClass()).isEqualTo(ResponsibilityClass.BUSINESS);
        assertThat(gardes.get(0).extractionCandidate()).isEqualTo(ExtractionCandidate.POLICY);
    }

    @Test
    void hasAdminPermission_doitEtreClassifieBusiness() {
        String source = """
                public class SecurityController {
                    private boolean hasAdminPermission() {
                        return currentUser != null && currentUser.getRoles().contains("ADMIN");
                    }
                }
                """;

        ExtractionResult result = adapter.extract("SecurityController.java", source);

        List<BusinessRule> gardes = result.rules().stream()
                .filter(r -> r.description().contains("hasAdminPermission"))
                .toList();
        assertThat(gardes).isNotEmpty();
        assertThat(gardes.get(0).responsibilityClass()).isEqualTo(ResponsibilityClass.BUSINESS);
        assertThat(gardes.get(0).extractionCandidate()).isEqualTo(ExtractionCandidate.POLICY);
    }

    @Test
    void onSave_retourVoid_neDoitPasEtreDetecteCommeGarde() {
        String source = """
                public class SaveController {
                    @FXML
                    public void onSave() {
                        service.save(entity);
                    }
                }
                """;

        ExtractionResult result = adapter.extract("SaveController.java", source);

        List<BusinessRule> gardes = result.rules().stream()
                .filter(r -> r.description().contains("Methode garde"))
                .toList();
        assertThat(gardes).isEmpty();
    }

    @Test
    void isValid_avecParametre_doitEtreClassifieBusinessEtDescriptionContiendreMetodeGarde() {
        String source = """
                public class ValidationController {
                    private boolean isValid(Patient patient) {
                        return patient != null && patient.getNom() != null;
                    }
                }
                """;

        ExtractionResult result = adapter.extract("ValidationController.java", source);

        List<BusinessRule> gardes = result.rules().stream()
                .filter(r -> r.description().contains("isValid"))
                .toList();
        assertThat(gardes).isNotEmpty();
        BusinessRule garde = gardes.get(0);
        assertThat(garde.responsibilityClass()).isEqualTo(ResponsibilityClass.BUSINESS);
        assertThat(garde.extractionCandidate()).isEqualTo(ExtractionCandidate.POLICY);
        assertThat(garde.description()).contains("Methode garde");
    }

    @Test
    void validate_sansPrefixeGarde_neDoitPasEtreGarde() {
        String source = """
                public class ValidatorController {
                    private boolean validate() {
                        return true;
                    }
                }
                """;

        ExtractionResult result = adapter.extract("ValidatorController.java", source);

        List<BusinessRule> gardes = result.rules().stream()
                .filter(r -> r.description().contains("Methode garde") && r.description().contains("validate"))
                .toList();
        // "validate" ne commence pas par un prefixe garde suivi d'une majuscule => pas une garde
        assertThat(gardes).isEmpty();
    }

    @Test
    void shouldShowWarning_doitEtreClassifieBusiness() {
        String source = """
                public class WarningController {
                    private boolean shouldShowWarning(Patient patient) {
                        return patient.getAge() > 70;
                    }
                }
                """;

        ExtractionResult result = adapter.extract("WarningController.java", source);

        List<BusinessRule> gardes = result.rules().stream()
                .filter(r -> r.description().contains("shouldShowWarning"))
                .toList();
        assertThat(gardes).isNotEmpty();
        assertThat(gardes.get(0).responsibilityClass()).isEqualTo(ResponsibilityClass.BUSINESS);
        assertThat(gardes.get(0).extractionCandidate()).isEqualTo(ExtractionCandidate.POLICY);
    }
}
