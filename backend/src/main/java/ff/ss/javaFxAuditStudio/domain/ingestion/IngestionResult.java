package ff.ss.javaFxAuditStudio.domain.ingestion;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record IngestionResult(List<SourceInput> inputs, List<IngestionError> errors) {

    public IngestionResult {
        requireNonNull(inputs, "inputs must not be null");
        requireNonNull(errors, "errors must not be null");
        inputs = List.copyOf(inputs);
        errors = List.copyOf(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
