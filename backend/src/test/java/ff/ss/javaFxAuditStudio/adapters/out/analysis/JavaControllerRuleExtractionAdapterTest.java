package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JavaControllerRuleExtractionAdapterTest {

    private static final String SAMPLE_CONTROLLER = """
            @Controller
            public class SampleController {
                @FXML private Button submitButton;
                @FXML private TextField nameField;
                @FXML private Label statusLabel;

                @Autowired private UserService userService;
                @Autowired private ValidationService validationService;

                @FXML
                public void handleSubmit() {
                    userService.save(nameField.getText());
                    statusLabel.setText("Saved");
                }

                @FXML
                public void handleReset() {
                    nameField.setText("");
                    statusLabel.setVisible(false);
                }

                public void handleCancel() {
                    getScene().close();
                }
            }
            """;

    private static final String UI_ONLY_CONTROLLER = """
            public class UiController {
                @FXML
                public void handleShow() {
                    label.setText("visible");
                }
            }
            """;

    private static final String BUSINESS_CONTROLLER = """
            public class BusinessController {
                @FXML
                public void handleSave() {
                    userService.save(entity);
                }
            }
            """;

    private JavaControllerRuleExtractionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JavaControllerRuleExtractionAdapter();
    }

    @Test
    void shouldExtractAtLeast3RulesFromSampleController() {
        List<BusinessRule> rules = adapter.extract("SampleController", SAMPLE_CONTROLLER);

        assertThat(rules).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void shouldReturnEmptyListWhenContentIsNull() {
        List<BusinessRule> rules = adapter.extract("SampleController", null);

        assertThat(rules).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenContentIsEmpty() {
        List<BusinessRule> rules = adapter.extract("SampleController", "");

        assertThat(rules).isEmpty();
    }

    @Test
    void shouldClassifyUiMethodsCorrectly() {
        List<BusinessRule> rules = adapter.extract("UiController", UI_ONLY_CONTROLLER);

        boolean hasUiRule = rules.stream()
                .anyMatch(r -> r.responsibilityClass() == ResponsibilityClass.UI);
        assertThat(hasUiRule).isTrue();
    }

    @Test
    void shouldClassifyBusinessMethodsCorrectly() {
        List<BusinessRule> rules = adapter.extract("BusinessController", BUSINESS_CONTROLLER);

        boolean hasBusinessOrApplicationRule = rules.stream()
                .anyMatch(r -> r.responsibilityClass() == ResponsibilityClass.BUSINESS
                        || r.responsibilityClass() == ResponsibilityClass.APPLICATION);
        assertThat(hasBusinessOrApplicationRule).isTrue();
    }

    @Test
    void shouldGenerateUniqueRuleIds() {
        List<BusinessRule> rules = adapter.extract("SampleController", SAMPLE_CONTROLLER);

        Set<String> ids = new HashSet<>();
        for (BusinessRule rule : rules) {
            ids.add(rule.ruleId());
        }
        assertThat(ids).hasSameSizeAs(rules);
    }

    @Test
    void shouldUseUnknownRefWhenControllerRefIsNull() {
        List<BusinessRule> rules = adapter.extract(null, UI_ONLY_CONTROLLER);

        assertThat(rules).allMatch(r -> r.sourceRef().equals("unknown"));
    }

    @Test
    void shouldMarkUnknownResponsibilityAsUncertain() {
        String content = """
                public class AController {
                    @FXML
                    public void handleAction() {
                        doSomethingUnknown();
                    }
                }
                """;
        List<BusinessRule> rules = adapter.extract("AController", content);

        boolean hasUncertain = rules.stream()
                .filter(r -> r.responsibilityClass() == ResponsibilityClass.UNKNOWN)
                .allMatch(BusinessRule::uncertain);
        assertThat(hasUncertain).isTrue();
    }

    @Test
    void shouldExtractFxmlFieldsAsUiRules() {
        String content = """
                public class FieldController {
                    @FXML private Button myButton;
                }
                """;
        List<BusinessRule> rules = adapter.extract("FieldController", content);

        assertThat(rules).anyMatch(r -> r.responsibilityClass() == ResponsibilityClass.UI);
    }
}
