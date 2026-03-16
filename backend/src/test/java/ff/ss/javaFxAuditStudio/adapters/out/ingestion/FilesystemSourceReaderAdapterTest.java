package ff.ss.javaFxAuditStudio.adapters.out.ingestion;

import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInput;
import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInputType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FilesystemSourceReaderAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void read_returnsSourceInput_whenJavaFileExists() throws IOException {
        Path javaFile;
        String content;
        FilesystemSourceReaderAdapter adapter;
        Optional<SourceInput> result;

        javaFile = Files.createTempFile(tempDir, "MyController", ".java");
        content = "public class MyController {}";
        Files.writeString(javaFile, content);
        adapter = new FilesystemSourceReaderAdapter();

        result = adapter.read(javaFile.toString());

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(SourceInputType.JAVA_CONTROLLER);
        assertThat(result.get().content()).isNotEmpty();
        assertThat(result.get().ref()).isEqualTo(javaFile.toString());
    }

    @Test
    void read_returnsSourceInput_whenFxmlFileExists() throws IOException {
        Path fxmlFile;
        String content;
        FilesystemSourceReaderAdapter adapter;
        Optional<SourceInput> result;

        fxmlFile = Files.createTempFile(tempDir, "view", ".fxml");
        content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><VBox/>";
        Files.writeString(fxmlFile, content);
        adapter = new FilesystemSourceReaderAdapter();

        result = adapter.read(fxmlFile.toString());

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(SourceInputType.FXML);
        assertThat(result.get().content()).isNotEmpty();
    }

    @Test
    void read_returnsSourceInput_whenPropertiesFileExists() throws IOException {
        Path propertiesFile;
        String content;
        FilesystemSourceReaderAdapter adapter;
        Optional<SourceInput> result;

        propertiesFile = Files.createTempFile(tempDir, "application", ".properties");
        content = "spring.datasource.url=jdbc:postgresql://localhost/db";
        Files.writeString(propertiesFile, content);
        adapter = new FilesystemSourceReaderAdapter();

        result = adapter.read(propertiesFile.toString());

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(SourceInputType.SPRING_PROPERTIES);
        assertThat(result.get().content()).isNotEmpty();
    }

    @Test
    void read_returnsEmpty_whenFileDoesNotExist() {
        Path nonExistentFile;
        FilesystemSourceReaderAdapter adapter;
        Optional<SourceInput> result;

        nonExistentFile = tempDir.resolve("NonExistent.java");
        adapter = new FilesystemSourceReaderAdapter();

        result = adapter.read(nonExistentFile.toString());

        assertThat(result).isEmpty();
    }

    @Test
    void read_returnsEmpty_whenExtensionUnsupported() throws IOException {
        Path txtFile;
        FilesystemSourceReaderAdapter adapter;
        Optional<SourceInput> result;

        txtFile = Files.createTempFile(tempDir, "notes", ".txt");
        Files.writeString(txtFile, "some content");
        adapter = new FilesystemSourceReaderAdapter();

        result = adapter.read(txtFile.toString());

        assertThat(result).isEmpty();
    }
}
