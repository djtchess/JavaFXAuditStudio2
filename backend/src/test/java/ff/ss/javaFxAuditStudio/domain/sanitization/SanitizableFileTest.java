package ff.ss.javaFxAuditStudio.domain.sanitization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires de SanitizableFile (QW-5).
 */
class SanitizableFileTest {

    @Test
    void should_create_nominal_file() {
        SanitizableFile file = new SanitizableFile("MyController.java", "class MyController {}", "java");

        assertThat(file.fileName()).isEqualTo("MyController.java");
        assertThat(file.content()).isEqualTo("class MyController {}");
        assertThat(file.fileType()).isEqualTo("java");
    }

    @Test
    void should_return_true_for_java_file_type() {
        SanitizableFile file = new SanitizableFile("Foo.java", "class Foo {}", "java");

        assertThat(file.isJava()).isTrue();
    }

    @Test
    void should_return_false_for_non_java_file_type() {
        SanitizableFile file = new SanitizableFile("application.yaml", "server:\n  port: 8080", "yaml");

        assertThat(file.isJava()).isFalse();
    }

    @Test
    void should_accept_empty_content() {
        SanitizableFile file = new SanitizableFile("empty.properties", "", "properties");

        assertThat(file.content()).isEmpty();
    }

    @Test
    void should_throw_npe_when_file_name_is_null() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SanitizableFile(null, "content", "java"))
                .withMessageContaining("fileName must not be null");
    }

    @Test
    void should_throw_npe_when_content_is_null() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SanitizableFile("file.java", null, "java"))
                .withMessageContaining("content must not be null");
    }

    @Test
    void should_throw_npe_when_file_type_is_null() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SanitizableFile("file.java", "content", null))
                .withMessageContaining("fileType must not be null");
    }

    @Test
    void should_throw_iae_when_file_name_is_blank() {
        assertThatThrownBy(() -> new SanitizableFile("   ", "content", "java"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fileName must not be blank");
    }

    @Test
    void should_throw_iae_when_file_type_is_blank() {
        assertThatThrownBy(() -> new SanitizableFile("file.java", "content", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fileType must not be blank");
    }

    @Test
    void should_be_case_insensitive_for_java_check() {
        SanitizableFile file = new SanitizableFile("Foo.JAVA", "content", "JAVA");

        assertThat(file.isJava()).isTrue();
    }
}
