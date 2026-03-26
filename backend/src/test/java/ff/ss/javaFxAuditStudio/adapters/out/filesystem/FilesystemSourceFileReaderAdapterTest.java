package ff.ss.javaFxAuditStudio.adapters.out.filesystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class FilesystemSourceFileReaderAdapterTest {

    private final FilesystemSourceFileReaderAdapter adapter = new FilesystemSourceFileReaderAdapter();

    @TempDir
    Path tempDir;

    @Test
    void read_should_return_file_content_when_file_exists() throws IOException {
        Path file = tempDir.resolve("MyController.java");
        Files.writeString(file, "public class MyController {}", StandardCharsets.UTF_8);

        assertThat(adapter.read(file.toString())).contains("public class MyController {}");
    }

    @Test
    void read_should_return_utf8_content_when_file_exists() throws IOException {
        Path file = tempDir.resolve("Utf8Controller.java");
        Files.writeString(file, "class Utf8Controller { String label = \"é\"; }", StandardCharsets.UTF_8);

        assertThat(adapter.read(file.toString()))
                .hasValueSatisfying(content -> assertThat(content).contains("é"));
    }

    @Test
    void read_should_return_empty_when_file_is_missing() {
        assertThat(adapter.read(tempDir.resolve("MissingController.java").toString())).isEmpty();
    }

    @Test
    void read_should_return_empty_when_path_is_invalid() {
        assertThat(adapter.read("bad\u0000path")).isEmpty();
    }

    @Test
    void read_should_return_empty_when_path_is_null() {
        assertThat(adapter.read(null)).isEmpty();
    }
}
