package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.domain.analysis.DependencyKind;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionResult;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaParserRuleExtractionAdapterDependencySignalsTest {

    @Test
    void extract_detectsInheritanceAndDynamicUiSignals() {
        JavaControllerRuleExtractionAdapter fallback = new JavaControllerRuleExtractionAdapter();
        JavaParserRuleExtractionAdapter adapter = new JavaParserRuleExtractionAdapter(fallback);
        String content = """
                public class RichController extends AbstractWorkflowController {
                    private BooleanProperty reviewVisible;

                    public void initialize() {
                        reviewPane.managedProperty().bind(reviewPane.visibleProperty());
                        reviewVisible.bind(formPane.visibleProperty());
                        reviewVisible.addListener((obs, oldValue, newValue) -> refreshButtons());
                        nextButton.setOnAction(event -> submit());
                    }
                }
                """;

        ExtractionResult result = adapter.extract("RichController.java", content);

        assertThat(result.dependencies()).anyMatch(dependency ->
                dependency.kind() == DependencyKind.INHERITANCE
                        && dependency.target().equals("AbstractWorkflowController"));
        assertThat(result.dependencies()).anyMatch(dependency ->
                dependency.kind() == DependencyKind.DYNAMIC_UI_VISIBILITY
                        && dependency.target().equals("reviewPane"));
        assertThat(result.dependencies()).anyMatch(dependency ->
                dependency.kind() == DependencyKind.DYNAMIC_UI_BINDING
                        && dependency.target().equals("reviewVisible"));
        assertThat(result.dependencies()).anyMatch(dependency ->
                dependency.kind() == DependencyKind.DYNAMIC_UI_LISTENER
                        && dependency.target().equals("reviewVisible"));
        assertThat(result.dependencies()).anyMatch(dependency ->
                dependency.kind() == DependencyKind.DYNAMIC_UI_EVENT_HANDLER
                        && dependency.target().equals("nextButton"));
    }
}
