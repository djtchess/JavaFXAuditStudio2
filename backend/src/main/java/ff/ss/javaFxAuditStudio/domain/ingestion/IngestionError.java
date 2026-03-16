package ff.ss.javaFxAuditStudio.domain.ingestion;

import static java.util.Objects.requireNonNull;

public record IngestionError(IngestionErrorCode code, String detail) {

    public IngestionError {
        requireNonNull(code, "code must not be null");
        requireNonNull(detail, "detail must not be null");
    }
}
