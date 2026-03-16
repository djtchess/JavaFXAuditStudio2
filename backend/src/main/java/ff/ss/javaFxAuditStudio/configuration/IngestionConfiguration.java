package ff.ss.javaFxAuditStudio.configuration;

import ff.ss.javaFxAuditStudio.adapters.out.ingestion.FilesystemSourceReaderAdapter;
import ff.ss.javaFxAuditStudio.application.ports.in.IngestSourcesUseCase;
import ff.ss.javaFxAuditStudio.application.service.IngestSourcesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IngestionConfiguration {

    @Bean
    public IngestSourcesUseCase ingestSourcesUseCase(final FilesystemSourceReaderAdapter filesystemSourceReaderAdapter) {
        IngestSourcesUseCase useCase;

        useCase = new IngestSourcesService(filesystemSourceReaderAdapter);
        return useCase;
    }
}
