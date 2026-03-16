package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInput;

import java.util.Optional;

public interface SourceReaderPort {

    Optional<SourceInput> read(String ref);
}
