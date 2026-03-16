package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.IngestSourcesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceReaderPort;
import ff.ss.javaFxAuditStudio.domain.ingestion.IngestionError;
import ff.ss.javaFxAuditStudio.domain.ingestion.IngestionErrorCode;
import ff.ss.javaFxAuditStudio.domain.ingestion.IngestionResult;
import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class IngestSourcesService implements IngestSourcesUseCase {

    private static final Logger log = LoggerFactory.getLogger(IngestSourcesService.class);

    private final SourceReaderPort sourceReaderPort;

    public IngestSourcesService(final SourceReaderPort sourceReaderPort) {
        this.sourceReaderPort = requireNonNull(sourceReaderPort, "sourceReaderPort must not be null");
    }

    @Override
    public IngestionResult handle(final List<String> sourceRefs) {
        requireNonNull(sourceRefs, "sourceRefs must not be null");
        log.debug("Ingestion demarree - {} refs", sourceRefs.size());

        List<SourceInput> inputs = new ArrayList<>();
        List<IngestionError> errors = new ArrayList<>();

        for (String ref : sourceRefs) {
            Optional<SourceInput> result = sourceReaderPort.read(ref);
            if (result.isEmpty()) {
                log.warn("Reference non trouvee - ref masquee par securite");
                errors.add(new IngestionError(IngestionErrorCode.FILE_NOT_FOUND, ref));
            } else {
                inputs.add(result.get());
            }
        }

        IngestionResult ingestionResult = new IngestionResult(inputs, errors);
        log.debug("Ingestion terminee - {} inputs, {} erreurs", ingestionResult.inputs().size(), ingestionResult.errors().size());
        return ingestionResult;
    }
}
