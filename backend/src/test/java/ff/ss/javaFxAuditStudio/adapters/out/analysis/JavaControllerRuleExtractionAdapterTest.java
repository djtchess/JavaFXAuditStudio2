package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionResult;
import ff.ss.javaFxAuditStudio.domain.rules.ParsingMode;
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
        ExtractionResult result = adapter.extract("SampleController", SAMPLE_CONTROLLER);

        assertThat(result.rules()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(result.parsingMode()).isEqualTo(ParsingMode.REGEX_FALLBACK);
    }

    @Test
    void shouldReturnEmptyListWhenContentIsNull() {
        ExtractionResult result = adapter.extract("SampleController", null);

        assertThat(result.rules()).isEmpty();
        assertThat(result.parsingMode()).isEqualTo(ParsingMode.REGEX_FALLBACK);
    }

    @Test
    void shouldReturnEmptyListWhenContentIsEmpty() {
        ExtractionResult result = adapter.extract("SampleController", "");

        assertThat(result.rules()).isEmpty();
        assertThat(result.parsingMode()).isEqualTo(ParsingMode.REGEX_FALLBACK);
    }

    @Test
    void shouldClassifyUiMethodsCorrectly() {
        ExtractionResult result = adapter.extract("UiController", UI_ONLY_CONTROLLER);

        boolean hasUiRule = result.rules().stream()
                .anyMatch(r -> r.responsibilityClass() == ResponsibilityClass.UI);
        assertThat(hasUiRule).isTrue();
    }

    @Test
    void shouldClassifyBusinessMethodsCorrectly() {
        ExtractionResult result = adapter.extract("BusinessController", BUSINESS_CONTROLLER);

        boolean hasBusinessOrApplicationRule = result.rules().stream()
                .anyMatch(r -> r.responsibilityClass() == ResponsibilityClass.BUSINESS
                        || r.responsibilityClass() == ResponsibilityClass.APPLICATION);
        assertThat(hasBusinessOrApplicationRule).isTrue();
    }

    @Test
    void shouldGenerateUniqueRuleIds() {
        ExtractionResult result = adapter.extract("SampleController", SAMPLE_CONTROLLER);

        List<BusinessRule> rules = result.rules();
        Set<String> ids = new HashSet<>();
        for (BusinessRule rule : rules) {
            ids.add(rule.ruleId());
        }
        assertThat(ids).hasSameSizeAs(rules);
    }

    @Test
    void shouldUseUnknownRefWhenControllerRefIsNull() {
        ExtractionResult result = adapter.extract(null, UI_ONLY_CONTROLLER);

        assertThat(result.rules()).allMatch(r -> r.sourceRef().equals("unknown"));
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
        ExtractionResult result = adapter.extract("AController", content);

        boolean hasUncertain = result.rules().stream()
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
        ExtractionResult result = adapter.extract("FieldController", content);

        assertThat(result.rules()).anyMatch(r -> r.responsibilityClass() == ResponsibilityClass.UI);
    }

    @Test
    void should_exclude_initialize_and_report_count() {
        JavaControllerRuleExtractionAdapter adapterWithExclusions =
                new JavaControllerRuleExtractionAdapter(Set.of("initialize"));
        String content = """
                public class MyController {
                    @FXML
                    public void initialize() {
                        label.setText("init");
                    }
                    @FXML
                    public void handleSave() {
                        service.save();
                    }
                }
                """;

        ExtractionResult result = adapterWithExclusions.extract("MyController", content);

        assertThat(result.excludedLifecycleMethodsCount()).isEqualTo(1);
        assertThat(result.rules()).noneMatch(r -> r.ruleId().contains("initialize"));
    }

    @Test
    void should_not_exclude_initializeSpecifique() {
        JavaControllerRuleExtractionAdapter adapterWithExclusions =
                new JavaControllerRuleExtractionAdapter(Set.of("initialize"));
        String content = """
                public class MyController {
                    @FXML
                    public void initializeSpecifique() {
                        doSetup();
                    }
                }
                """;

        ExtractionResult result = adapterWithExclusions.extract("MyController", content);

        assertThat(result.excludedLifecycleMethodsCount()).isEqualTo(0);
        assertThat(result.rules()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void should_count_multiple_excluded_methods() {
        JavaControllerRuleExtractionAdapter adapterWithExclusions =
                new JavaControllerRuleExtractionAdapter(Set.of("initialize", "dispose", "stop"));
        String content = """
                public class MyController {
                    @FXML
                    public void initialize() { }
                    @FXML
                    public void dispose() { }
                    @FXML
                    public void stop() { }
                    @FXML
                    public void handleAction() { doAction(); }
                }
                """;

        ExtractionResult result = adapterWithExclusions.extract("MyController", content);

        assertThat(result.excludedLifecycleMethodsCount()).isEqualTo(3);
    }

    @Test
    void should_return_zero_count_when_no_lifecycle_method() {
        JavaControllerRuleExtractionAdapter adapterWithExclusions =
                new JavaControllerRuleExtractionAdapter(Set.of("initialize", "dispose"));
        String content = """
                public class MyController {
                    @FXML
                    public void handleSave() { service.save(); }
                    @FXML
                    public void handleCancel() { close(); }
                }
                """;

        ExtractionResult result = adapterWithExclusions.extract("MyController", content);

        assertThat(result.excludedLifecycleMethodsCount()).isEqualTo(0);
    }
}
