package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactValidationWarning;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour ArtifactCompilabilityValidator (JAS-009).
 */
class ArtifactCompilabilityValidatorTest {

    private ArtifactCompilabilityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ArtifactCompilabilityValidator();
    }

    @Test
    void shouldReturnOkStatusForValidInterface() {
        CodeArtifact artifact = artifact("public interface FooUseCase {\n    void save();\n    void delete();\n}");

        CodeArtifact result = validator.validate(artifact);

        assertThat(result.generationStatus()).isEqualTo("OK");
        assertThat(result.generationWarnings()).isEmpty();
    }

    @Test
    void shouldReturnOkStatusForValidClass() {
        String content = "import javafx.fxml.FXML;\n"
                + "@org.springframework.stereotype.Component\n"
                + "public class FooController {\n"
                + "    @FXML public void save() {}\n"
                + "    @FXML public void delete() {}\n"
                + "}\n";
        CodeArtifact artifact = artifact(content);

        CodeArtifact result = validator.validate(artifact);

        assertThat(result.generationStatus()).isEqualTo("OK");
        assertThat(result.generationWarnings()).doesNotContain(ArtifactValidationWarning.DUPLICATE_METHOD_NAME);
    }

    @Test
    void shouldDetectDuplicateMethodNames() {
        String content = "public class FooController {\n"
                + "    public void save() {}\n"
                + "    public void save() {}\n"
                + "}\n";
        CodeArtifact artifact = artifact(content);

        CodeArtifact result = validator.validate(artifact);

        assertThat(result.generationWarnings()).contains(ArtifactValidationWarning.DUPLICATE_METHOD_NAME);
        assertThat(result.generationStatus()).isEqualTo("WARNING");
    }

    @Test
    void shouldDetectParseError() {
        CodeArtifact artifact = artifact("this is not valid java {{{{");

        CodeArtifact result = validator.validate(artifact);

        assertThat(result.generationWarnings()).contains(ArtifactValidationWarning.PARSE_ERROR);
        assertThat(result.generationStatus()).isEqualTo("WARNING");
    }

    @Test
    void shouldDetectEmptyBodyForEmptyInterface() {
        CodeArtifact artifact = artifact("public interface EmptyUseCase {}");

        CodeArtifact result = validator.validate(artifact);

        assertThat(result.generationWarnings()).contains(ArtifactValidationWarning.EMPTY_BODY);
    }

    @Test
    void shouldPreserveAllOriginalArtifactFields() {
        CodeArtifact original = artifact("public interface FooUseCase {\n    void save();\n}");

        CodeArtifact result = validator.validate(original);

        assertThat(result.artifactId()).isEqualTo(original.artifactId());
        assertThat(result.type()).isEqualTo(original.type());
        assertThat(result.lotNumber()).isEqualTo(original.lotNumber());
        assertThat(result.className()).isEqualTo(original.className());
        assertThat(result.content()).isEqualTo(original.content());
        assertThat(result.transitionalBridge()).isEqualTo(original.transitionalBridge());
    }

    private CodeArtifact artifact(final String content) {
        return new CodeArtifact(
                "test-lot2-use_case",
                ArtifactType.USE_CASE,
                2,
                "FooUseCase",
                content,
                false);
    }
}
