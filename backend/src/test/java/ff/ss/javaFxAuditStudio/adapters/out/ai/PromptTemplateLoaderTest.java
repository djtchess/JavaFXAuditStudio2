package ff.ss.javaFxAuditStudio.adapters.out.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires de {@link PromptTemplateLoader}.
 *
 * <p>Verifie le rendu des templates Mustache depuis le classpath de test
 * et le comportement de fallback sur enrichment-default.
 */
class PromptTemplateLoaderTest {

    private PromptTemplateLoader loader;

    @BeforeEach
    void setUp() {
        loader = new PromptTemplateLoader();
    }

    @Test
    void should_render_naming_template_with_controller_ref() {
        Map<String, Object> context = Map.of(
                "controllerRef", "Component_Test",
                "sanitizedSource", "@FXML void onAction() { component.save(data); }",
                "estimatedTokens", 42,
                "taskType", "NAMING");

        String rendered = loader.render("enrichment-naming", context);

        assertThat(rendered).contains("Component_Test");
    }

    @Test
    void should_render_description_template() {
        Map<String, Object> context = Map.of(
                "controllerRef", "Component_Desc",
                "sanitizedSource", "@FXML void onAction() { label.setText(value); }",
                "estimatedTokens", 30,
                "taskType", "DESCRIPTION");

        String rendered = loader.render("enrichment-description", context);

        assertThat(rendered).contains("Component_Desc");
        assertThat(rendered).contains("business description");
    }

    @Test
    void should_fallback_to_default_template_when_not_found() {
        Map<String, Object> context = Map.of(
                "controllerRef", "Component_Fallback",
                "sanitizedSource", "@FXML void onAction() { }",
                "estimatedTokens", 10,
                "taskType", "UNKNOWN_TASK");

        String rendered = loader.render("enrichment-nonexistent-template", context);

        assertThat(rendered).contains("Component_Fallback");
        assertThat(rendered).contains("UNKNOWN_TASK");
    }

    @Test
    void should_include_sanitized_source_in_render() {
        String source = "@FXML void onAction() { component.compute(input); }";
        Map<String, Object> context = Map.of(
                "controllerRef", "Component_Source",
                "sanitizedSource", source,
                "estimatedTokens", 20,
                "taskType", "NAMING");

        String rendered = loader.render("enrichment-naming", context);

        assertThat(rendered).contains(source);
    }

    @Test
    void should_render_spring_boot_generation_template_without_todo_instruction() {
        Map<String, Object> context = Map.of(
                "controllerRef", "Component_Gen",
                "sanitizedSource", "class Controller { void save() { service.save(); } }",
                "estimatedTokens", 64,
                "classifiedRules", "[RG-001] save -> USE_CASE / APPLICATION",
                "screenContext", "Session: sess-1",
                "reclassificationFeedback", "Aucune reclassification",
                "ruleSourceSnippets", "save() -> service.save();",
                "projectReferencePatterns", "PatientUseCase -> save()");

        String rendered = loader.render("spring-boot-generation", context);

        assertThat(rendered)
                .contains("Ne jamais laisser de placeholder")
                .doesNotContain("Laisser un commentaire `// TODO: implementer`")
                .contains("return true;");
    }
}
