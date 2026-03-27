package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.ai.AiArtifactZipExport;

/**
 * Export ZIP des artefacts IA persistés.
 */
public interface ExportAiGeneratedArtifactsUseCase {

    AiArtifactZipExport export(String sessionId);
}
