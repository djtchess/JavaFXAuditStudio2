package ff.ss.javaFxAuditStudio.application.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ff.ss.javaFxAuditStudio.application.ports.in.ExportAiGeneratedArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ListAiGeneratedArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AiArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiArtifactZipExport;
import ff.ss.javaFxAuditStudio.domain.ai.AiArtifactImplementationInspector;
import ff.ss.javaFxAuditStudio.domain.ai.AiGeneratedArtifact;

/**
 * Consultation et export des artefacts IA persistés.
 */
public class AiArtifactCatalogService implements ListAiGeneratedArtifactsUseCase, ExportAiGeneratedArtifactsUseCase {

    private static final Pattern PACKAGE_DECLARATION = Pattern.compile(
            "(?m)^\\s*package\\s+([a-zA-Z_][a-zA-Z0-9_.]*)\\s*;");

    private final AnalysisSessionPort sessionPort;
    private final AiArtifactPersistencePort aiArtifactPersistencePort;

    public AiArtifactCatalogService(
            final AnalysisSessionPort sessionPort,
            final AiArtifactPersistencePort aiArtifactPersistencePort) {
        this.sessionPort = Objects.requireNonNull(sessionPort, "sessionPort must not be null");
        this.aiArtifactPersistencePort = Objects.requireNonNull(
                aiArtifactPersistencePort,
                "aiArtifactPersistencePort must not be null");
    }

    @Override
    public List<AiGeneratedArtifact> listLatest(final String sessionId) {
        ensureSessionExists(sessionId);
        return aiArtifactPersistencePort.findLatestBySessionId(sessionId);
    }

    @Override
    public List<AiGeneratedArtifact> listVersions(final String sessionId, final String artifactType) {
        ensureSessionExists(sessionId);
        return aiArtifactPersistencePort.findVersions(sessionId, artifactType);
    }

    @Override
    public AiArtifactZipExport export(final String sessionId) {
        List<AiGeneratedArtifact> artifacts = resolveExportableArtifacts(sessionId);
        if (artifacts.isEmpty()) {
            throw new IllegalStateException("Aucun artefact IA exportable pour la session : " + sessionId);
        }

        byte[] zipContent = buildZip(artifacts);
        return new AiArtifactZipExport("ai-artifacts-" + sessionId + ".zip", zipContent, artifacts.size());
    }

    private void ensureSessionExists(final String sessionId) {
        sessionPort.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
    }

    private List<AiGeneratedArtifact> resolveExportableArtifacts(final String sessionId) {
        List<AiGeneratedArtifact> artifacts = listLatest(sessionId);
        return artifacts.stream()
                .filter(artifact -> !AiArtifactImplementationInspector.isIncomplete(artifact.content()))
                .toList();
    }

    private byte[] buildZip(final List<AiGeneratedArtifact> artifacts) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            for (AiGeneratedArtifact artifact : artifacts) {
                ZipEntry entry = new ZipEntry(resolveZipEntryName(artifact));
                zipOutputStream.putNextEntry(entry);
                zipOutputStream.write(artifact.content().getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de construire l'archive ZIP des artefacts IA", exception);
        }
    }

    private String resolveZipEntryName(final AiGeneratedArtifact artifact) {
        String entryName = artifact.className() + ".java";
        Matcher matcher = PACKAGE_DECLARATION.matcher(artifact.content());
        if (matcher.find()) {
            entryName = matcher.group(1).replace('.', '/') + "/" + entryName;
        }
        return entryName;
    }
}
