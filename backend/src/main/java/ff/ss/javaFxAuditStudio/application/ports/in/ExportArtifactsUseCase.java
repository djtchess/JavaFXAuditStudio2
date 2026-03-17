package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.generation.ExportResult;

public interface ExportArtifactsUseCase {
    ExportResult export(String sessionId, String targetDirectory);
}
