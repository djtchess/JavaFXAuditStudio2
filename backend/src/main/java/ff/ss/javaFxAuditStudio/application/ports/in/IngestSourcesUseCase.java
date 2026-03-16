package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.ingestion.IngestionResult;

import java.util.List;

public interface IngestSourcesUseCase {

    IngestionResult handle(List<String> sourceRefs);
}
