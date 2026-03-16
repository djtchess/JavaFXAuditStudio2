package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.List;
import java.util.Objects;

public record SubmitAnalysisRequest(
        List<String> sourceFilePaths,
        String sessionName) {

    public SubmitAnalysisRequest {
        Objects.requireNonNull(sourceFilePaths, "sourceFilePaths must not be null");
        Objects.requireNonNull(sessionName, "sessionName must not be null");
        if (sourceFilePaths.isEmpty()) {
            throw new IllegalArgumentException("sourceFilePaths must not be empty");
        }
        sourceFilePaths = List.copyOf(sourceFilePaths);
    }
}
