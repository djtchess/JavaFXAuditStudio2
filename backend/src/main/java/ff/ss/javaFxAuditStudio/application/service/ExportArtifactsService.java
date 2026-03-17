package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.ExportArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.ExportResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExportArtifactsService implements ExportArtifactsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExportArtifactsService.class);

    private final ArtifactPersistencePort artifactPersistencePort;

    public ExportArtifactsService(final ArtifactPersistencePort artifactPersistencePort) {
        this.artifactPersistencePort = Objects.requireNonNull(artifactPersistencePort);
    }

    @Override
    public ExportResult export(final String sessionId, final String targetDirectory) {
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(targetDirectory);

        Path targetDir = Path.of(targetDirectory);
        List<String> exportedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            log.error("Impossible de creer le repertoire cible - path={}", targetDirectory, e);
            return new ExportResult(targetDirectory, List.of(), List.of("Impossible de creer le repertoire : " + e.getMessage()));
        }

        List<CodeArtifact> artifacts = artifactPersistencePort
                .findBySessionId(sessionId)
                .map(r -> r.artifacts())
                .orElse(List.of());

        if (artifacts.isEmpty()) {
            return new ExportResult(targetDirectory, List.of(), List.of("Aucun artefact trouve pour la session " + sessionId));
        }

        for (CodeArtifact artifact : artifacts) {
            String fileName = artifact.className() + ".java";
            Path filePath = targetDir.resolve(fileName);
            String content = artifact.content() != null ? artifact.content() : "// Contenu non genere\n";
            try {
                Files.writeString(filePath, content, StandardCharsets.UTF_8);
                exportedFiles.add(filePath.toString());
                log.debug("Artefact exporte - file={}", filePath);
            } catch (IOException e) {
                log.error("Echec export artefact - file={}", filePath, e);
                errors.add("Echec pour " + fileName + " : " + e.getMessage());
            }
        }

        log.info("Export termine - {} fichiers ecrits, {} erreurs, dir={}", exportedFiles.size(), errors.size(), targetDirectory);
        return new ExportResult(targetDirectory, exportedFiles, errors);
    }
}
