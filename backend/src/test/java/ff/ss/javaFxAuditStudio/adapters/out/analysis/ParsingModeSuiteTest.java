package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.configuration.AnalysisProperties;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionResult;
import ff.ss.javaFxAuditStudio.domain.rules.ParsingMode;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ParsingModeSuiteTest {

    /** Patterns par defaut : tous null => les methodes effective*() retournent leurs valeurs par defaut. */
    private static final AnalysisProperties.ClassificationPatterns DEFAULT_PATTERNS =
            new AnalysisProperties.ClassificationPatterns(
                    null, null, null, null, null, null, null, null);

    private static String loadSample(final String name) throws IOException, URISyntaxException {
        var url = ParsingModeSuiteTest.class.getClassLoader().getResource("samples/" + name);
        assertThat(url).as("Sample %s introuvable dans src/test/resources/samples/", name).isNotNull();
        return Files.readString(Path.of(url.toURI()));
    }

    @Test
    void astMode_shouldParseValidJavaWithoutFallback() throws Exception {
        String content = loadSample("SampleController.java");
        var regex = new JavaControllerRuleExtractionAdapter();
        var adapter = new JavaParserRuleExtractionAdapter(regex);

        ExtractionResult result = adapter.extract("SampleController.java", content);

        assertThat(result.parsingMode()).isEqualTo(ParsingMode.AST);
        assertThat(result.rules()).isNotEmpty();
    }

    @Test
    void regexMode_shouldFallbackOnInvalidJava() {
        String invalidContent = "import .business.*;\npublic class Broken { }";
        var regex = new JavaControllerRuleExtractionAdapter();
        var adapter = new JavaParserRuleExtractionAdapter(regex);

        ExtractionResult result = adapter.extract("Broken.java", invalidContent);

        assertThat(result.parsingMode()).isEqualTo(ParsingMode.REGEX_FALLBACK);
    }

    @Test
    void astMode_unknownRateShouldBeLow() throws Exception {
        String content = loadSample("SampleController.java");
        var regex = new JavaControllerRuleExtractionAdapter();
        var adapter = new JavaParserRuleExtractionAdapter(regex);

        ExtractionResult result = adapter.extract("SampleController.java", content);
        List<BusinessRule> rules = result.rules();
        long unknownCount = rules.stream()
                .filter(r -> r.responsibilityClass() == ResponsibilityClass.UNKNOWN)
                .count();
        double unknownRate = rules.isEmpty() ? 0 : (double) unknownCount / rules.size();

        assertThat(unknownRate)
                .as("Taux UNKNOWN en mode AST ne doit pas depasser 25%%")
                .isLessThanOrEqualTo(0.25);
    }

    @Test
    void regexMode_unknownRateShouldBeLow() throws Exception {
        String content = loadSample("SampleController.java");
        var regex = new JavaControllerRuleExtractionAdapter();

        ExtractionResult result = regex.extract("SampleController.java", content);
        List<BusinessRule> rules = result.rules();
        long unknownCount = rules.stream()
                .filter(r -> r.responsibilityClass() == ResponsibilityClass.UNKNOWN)
                .count();
        double unknownRate = rules.isEmpty() ? 0 : (double) unknownCount / rules.size();

        assertThat(unknownRate)
                .as("Taux UNKNOWN en mode regex ne doit pas depasser 25%%")
                .isLessThanOrEqualTo(0.25);
    }

    @Test
    void lifecycleMethod_shouldBeFilteredInBothModes() throws Exception {
        String content = loadSample("SampleController.java");
        Set<String> lifecycle = Set.of("initialize", "dispose", "stop");

        // JavaControllerRuleExtractionAdapter n'a pas de constructeur (Set<String>) seul :
        // le constructeur principal prend (Set<String>, ClassificationPatterns).
        var regex = new JavaControllerRuleExtractionAdapter(lifecycle, DEFAULT_PATTERNS);
        // JavaParserRuleExtractionAdapter expose bien (RuleExtractionPort, Set<String>).
        var adapter = new JavaParserRuleExtractionAdapter(regex, lifecycle);

        ExtractionResult astResult = adapter.extract("SampleController.java", content);
        ExtractionResult regexResult = regex.extract("SampleController.java", content);

        // Aucune regle ne doit mentionner "handler initialize" dans sa description.
        assertThat(astResult.rules())
                .noneMatch(r -> r.description().contains("handler initialize"));
        assertThat(regexResult.rules())
                .noneMatch(r -> r.description().contains("handler initialize"));
    }

    @Test
    void astMode_should_classify_ui_boolean_guard_as_ui_instead_of_policy() {
        String content = """
                import javafx.fxml.FXML;
                import javafx.scene.control.Button;

                public class SampleController {
                    @FXML
                    private Button acquisitionButton;

                    public boolean isNotDisabledAcquisitionButton() {
                        return !acquisitionButton.isDisable();
                    }
                }
                """;
        var regex = new JavaControllerRuleExtractionAdapter(Set.of(), DEFAULT_PATTERNS);
        var adapter = new JavaParserRuleExtractionAdapter(regex, Set.of(), DEFAULT_PATTERNS);

        ExtractionResult result = adapter.extract("SampleController.java", content);

        assertThat(result.rules())
                .anyMatch(rule -> rule.description().contains("isNotDisabledAcquisitionButton")
                        && rule.responsibilityClass() == ResponsibilityClass.UI);
    }

    @Test
    void regexMode_should_classify_ui_boolean_guard_as_ui_instead_of_policy() {
        String content = """
                import javafx.fxml.FXML;
                import javafx.scene.control.Button;

                public class SampleController {
                    @FXML
                    private Button acquisitionButton;

                    public boolean isNotDisabledAcquisitionButton() {
                        return !acquisitionButton.isDisable();
                    }
                }
                """;
        var regex = new JavaControllerRuleExtractionAdapter(Set.of(), DEFAULT_PATTERNS);

        ExtractionResult result = regex.extract("SampleController.java", content);

        assertThat(result.rules())
                .anyMatch(rule -> rule.description().contains("isNotDisabledAcquisitionButton")
                        && rule.responsibilityClass() == ResponsibilityClass.UI);
    }
}
