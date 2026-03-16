package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.domain.cartography.FxmlComponent;
import ff.ss.javaFxAuditStudio.domain.cartography.HandlerBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class FxmlCartographyAnalysisAdapterTest {

    private FxmlCartographyAnalysisAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FxmlCartographyAnalysisAdapter();
    }

    // --- extractComponents ---

    @Test
    void extractComponents_returnsTwoComponents_whenSampleViewFxmlLoaded() throws IOException {
        String fxmlContent;
        fxmlContent = loadFixture("/fixtures/SampleView.fxml");
        List<FxmlComponent> components;

        components = adapter.extractComponents(fxmlContent);

        // SampleView.fxml contient 4 éléments avec fx:id :
        // nameField (TextField), submitButton (Button), resetButton (Button), statusLabel (Label)
        assertThat(components).hasSize(4);
    }

    @Test
    void extractComponents_returnsComponentsWithCorrectFxIds_whenSampleViewFxmlLoaded() throws IOException {
        String fxmlContent;
        fxmlContent = loadFixture("/fixtures/SampleView.fxml");
        List<FxmlComponent> components;

        components = adapter.extractComponents(fxmlContent);

        List<String> fxIds;
        fxIds = components.stream().map(FxmlComponent::fxId).toList();
        assertThat(fxIds).containsExactlyInAnyOrder("nameField", "submitButton", "resetButton", "statusLabel");
    }

    @Test
    void extractComponents_returnsHandlers_forButtonsWithOnAction() throws IOException {
        String fxmlContent;
        fxmlContent = loadFixture("/fixtures/SampleView.fxml");
        List<FxmlComponent> components;

        components = adapter.extractComponents(fxmlContent);

        long handlersFound;
        handlersFound = components.stream()
                .filter(c -> !c.eventHandler().isEmpty())
                .count();
        // submitButton -> handleSubmit, resetButton -> handleReset
        assertThat(handlersFound).isEqualTo(2L);
    }

    @Test
    void extractComponents_stripsHashPrefix_fromEventHandler() throws IOException {
        String fxmlContent;
        fxmlContent = loadFixture("/fixtures/SampleView.fxml");
        List<FxmlComponent> components;

        components = adapter.extractComponents(fxmlContent);

        FxmlComponent submitButton;
        submitButton = components.stream()
                .filter(c -> "submitButton".equals(c.fxId()))
                .findFirst()
                .orElseThrow();
        assertThat(submitButton.eventHandler()).isEqualTo("handleSubmit");
    }

    @Test
    void extractComponents_returnsEmptyList_whenFxmlContentIsEmpty() {
        List<FxmlComponent> components;

        components = adapter.extractComponents("");

        assertThat(components).isEmpty();
    }

    @Test
    void extractComponents_returnsEmptyList_whenFxmlContentIsNull() {
        List<FxmlComponent> components;

        components = adapter.extractComponents(null);

        assertThat(components).isEmpty();
    }

    @Test
    void extractComponents_returnsEmptyList_whenFxmlContentIsBlank() {
        List<FxmlComponent> components;

        components = adapter.extractComponents("   ");

        assertThat(components).isEmpty();
    }

    @Test
    void extractComponents_doesNotThrow_whenFxmlIsInvalid() {
        assertThatCode(() -> adapter.extractComponents("<invalid>"))
                .doesNotThrowAnyException();
    }

    @Test
    void extractComponents_returnsParseError_whenFxmlIsInvalid() {
        List<FxmlComponent> components;

        components = adapter.extractComponents("<invalid>");

        assertThat(components).isNotEmpty();
        boolean hasParseError;
        hasParseError = components.stream().anyMatch(c -> "PARSE_ERROR".equals(c.fxId()));
        assertThat(hasParseError).isTrue();
    }

    @Test
    void extractComponents_returnsMinimalComponents_whenComplexViewFxmlLoaded() throws IOException {
        String fxmlContent;
        fxmlContent = loadFixture("/fixtures/ComplexView.fxml");
        List<FxmlComponent> components;

        components = adapter.extractComponents(fxmlContent);

        // ComplexView.fxml contient 6+ éléments avec fx:id
        assertThat(components).hasSizeGreaterThanOrEqualTo(6);
    }

    @Test
    void extractComponents_returnsMinimalHandlers_whenComplexViewFxmlLoaded() throws IOException {
        String fxmlContent;
        fxmlContent = loadFixture("/fixtures/ComplexView.fxml");
        List<FxmlComponent> components;

        components = adapter.extractComponents(fxmlContent);

        long handlersFound;
        handlersFound = components.stream()
                .filter(c -> !c.eventHandler().isEmpty())
                .count();
        // ComplexView.fxml contient 4+ onAction
        assertThat(handlersFound).isGreaterThanOrEqualTo(4L);
    }

    // --- extractHandlers ---

    @Test
    void extractHandlers_returnsThreeHandlers_whenSampleControllerLoaded() throws IOException {
        String javaContent;
        javaContent = loadFixture("/fixtures/SampleController.java");
        List<HandlerBinding> handlers;

        handlers = adapter.extractHandlers(javaContent);

        // SampleController.java contient 3 méthodes @FXML void : handleSubmit, handleReset, handleClose
        assertThat(handlers).hasSize(3);
    }

    @Test
    void extractHandlers_returnsCorrectMethodNames_whenSampleControllerLoaded() throws IOException {
        String javaContent;
        javaContent = loadFixture("/fixtures/SampleController.java");
        List<HandlerBinding> handlers;

        handlers = adapter.extractHandlers(javaContent);

        List<String> methodNames;
        methodNames = handlers.stream().map(HandlerBinding::methodName).toList();
        assertThat(methodNames).containsExactlyInAnyOrder("handleSubmit", "handleReset", "handleClose");
    }

    @Test
    void extractHandlers_setsUnknownFxmlRef_forAllHandlers() throws IOException {
        String javaContent;
        javaContent = loadFixture("/fixtures/SampleController.java");
        List<HandlerBinding> handlers;

        handlers = adapter.extractHandlers(javaContent);

        boolean allUnknown;
        allUnknown = handlers.stream().allMatch(h -> "unknown".equals(h.fxmlRef()));
        assertThat(allUnknown).isTrue();
    }

    @Test
    void extractHandlers_setsVoidInjectedType_forAllHandlers() throws IOException {
        String javaContent;
        javaContent = loadFixture("/fixtures/SampleController.java");
        List<HandlerBinding> handlers;

        handlers = adapter.extractHandlers(javaContent);

        boolean allVoid;
        allVoid = handlers.stream().allMatch(h -> "void".equals(h.injectedType()));
        assertThat(allVoid).isTrue();
    }

    @Test
    void extractHandlers_returnsEmptyList_whenJavaContentIsNull() {
        List<HandlerBinding> handlers;

        handlers = adapter.extractHandlers(null);

        assertThat(handlers).isEmpty();
    }

    @Test
    void extractHandlers_returnsEmptyList_whenJavaContentIsEmpty() {
        List<HandlerBinding> handlers;

        handlers = adapter.extractHandlers("");

        assertThat(handlers).isEmpty();
    }

    @Test
    void extractHandlers_returnsEmptyList_whenJavaContentIsBlank() {
        List<HandlerBinding> handlers;

        handlers = adapter.extractHandlers("   ");

        assertThat(handlers).isEmpty();
    }

    @Test
    void extractHandlers_returnsMinimalHandlers_whenComplexControllerLoaded() throws IOException {
        String javaContent;
        javaContent = loadFixture("/fixtures/ComplexController.java");
        List<HandlerBinding> handlers;

        handlers = adapter.extractHandlers(javaContent);

        // ComplexController.java contient 8+ méthodes @FXML void
        assertThat(handlers).hasSizeGreaterThanOrEqualTo(8);
    }

    // --- utilitaire ---

    private String loadFixture(final String resourcePath) throws IOException {
        InputStream stream;
        stream = getClass().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IOException("Fixture introuvable : " + resourcePath);
        }
        byte[] bytes;
        bytes = stream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
