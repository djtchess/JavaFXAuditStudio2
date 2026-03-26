package ff.ss.javaFxAuditStudio.adapters.out.filesystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;

/**
 * Hexagonal adapter that reads source files from the filesystem.
 */
public class FilesystemSourceFileReaderAdapter implements SourceFileReaderPort {

    private static final Logger LOG = LoggerFactory.getLogger(FilesystemSourceFileReaderAdapter.class);

    @Override
    public Optional<String> read(final String filePath) {
        if (filePath == null) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
            LOG.debug("Source read from {} : {} characters", filePath, content.length());
            return Optional.of(content);
        } catch (IOException | InvalidPathException e) {
            LOG.warn("Unable to read {} - fallback to controller reference: {}", filePath, e.getMessage());
            return Optional.empty();
        }
    }
}
