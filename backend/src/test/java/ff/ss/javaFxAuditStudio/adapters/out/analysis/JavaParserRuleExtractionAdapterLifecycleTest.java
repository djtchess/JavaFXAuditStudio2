package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.domain.rules.ExtractionResult;
import ff.ss.javaFxAuditStudio.domain.rules.ParsingMode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests du filtrage lifecycle en mode AST (JavaParser).
 * Verifie que les methodes lifecycle exclues sont comptees sans etre incluses dans les regles.
 */
class JavaParserRuleExtractionAdapterLifecycleTest {

    private JavaParserRuleExtractionAdapter adapterWithExclusions(final Set<String> excluded) {
        JavaControllerRuleExtractionAdapter fallback = new JavaControllerRuleExtractionAdapter(excluded);
        return new JavaParserRuleExtractionAdapter(fallback, excluded);
    }

    @Test
    void should_exclude_initialize_in_ast_mode() {
        JavaParserRuleExtractionAdapter adapter = adapterWithExclusions(Set.of("initialize"));
        String javaSource = """
                public class FooController {
                    @FXML void initialize() { label.setText("init"); }
                    @FXML void onSave() { service.save(); }
                }
                """;

        ExtractionResult result = adapter.extract("FooController", javaSource);

        assertThat(result.parsingMode()).isEqualTo(ParsingMode.AST);
        assertThat(result.excludedLifecycleMethodsCount()).isEqualTo(1);
        assertThat(result.rules()).noneMatch(r -> r.description().contains("initialize"));
    }

    @Test
    void should_exclude_dispose_in_ast_mode() {
        JavaParserRuleExtractionAdapter adapter = adapterWithExclusions(Set.of("dispose"));
        String javaSource = """
                public class BarController {
                    @FXML void dispose() { cleanup(); }
                    @FXML void handleOk() { form.submit(); }
                }
                """;

        ExtractionResult result = adapter.extract("BarController", javaSource);

        assertThat(result.parsingMode()).isEqualTo(ParsingMode.AST);
        assertThat(result.excludedLifecycleMethodsCount()).isEqualTo(1);
        assertThat(result.rules()).noneMatch(r -> r.description().contains("dispose"));
    }

    @Test
    void should_not_exclude_initializeSpecifique_in_ast_mode() {
        JavaParserRuleExtractionAdapter adapter = adapterWithExclusions(Set.of("initialize"));
        String javaSource = """
                public class BazController {
                    @FXML void initializeSpecifique() { setup(); }
                }
                """;

        ExtractionResult result = adapter.extract("BazController", javaSource);

        assertThat(result.parsingMode()).isEqualTo(ParsingMode.AST);
        assertThat(result.excludedLifecycleMethodsCount()).isEqualTo(0);
        assertThat(result.rules()).anyMatch(r -> r.description().contains("initializeSpecifique"));
    }

    @Test
    void should_count_excluded_lifecycle_methods_in_ast_mode() {
        JavaParserRuleExtractionAdapter adapter =
                adapterWithExclusions(Set.of("initialize", "dispose", "stop"));
        String javaSource = """
                public class MultiController {
                    @FXML void initialize() { }
                    @FXML void dispose() { }
                    @FXML void stop() { }
                    @FXML void onAction() { doAction(); }
                }
                """;

        ExtractionResult result = adapter.extract("MultiController", javaSource);

        assertThat(result.parsingMode()).isEqualTo(ParsingMode.AST);
        assertThat(result.excludedLifecycleMethodsCount()).isEqualTo(3);
    }
}
