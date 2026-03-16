package ff.ss.javaFxAuditStudio.domain.ingestion;

import static java.util.Objects.requireNonNull;

public record SourceInput(String ref, SourceInputType type, String content) {

    public SourceInput {
        requireNonNull(ref, "ref must not be null");
        requireNonNull(type, "type must not be null");
        requireNonNull(content, "content must not be null");
        if (ref.isBlank()) {
            throw new IllegalArgumentException("ref must not be blank");
        }
    }
}
