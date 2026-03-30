package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.ExportArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AiArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.domain.ai.AiArtifactImplementationInspector;
import ff.ss.javaFxAuditStudio.domain.ai.AiGeneratedArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.ExportResult;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExportArtifactsService implements ExportArtifactsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExportArtifactsService.class);
    private static final Pattern PACKAGE_DECLARATION = Pattern.compile(
            "(?m)^\\s*package\\s+([a-zA-Z_][a-zA-Z0-9_.]*)\\s*;");
    private static final String MAIN_SOURCE_ROOT = "src/main/java";
    private static final String TEST_SOURCE_ROOT = "src/test/java";

    private final ArtifactPersistencePort artifactPersistencePort;
    private final AiArtifactPersistencePort aiArtifactPersistencePort;

    public ExportArtifactsService(final ArtifactPersistencePort artifactPersistencePort) {
        this(artifactPersistencePort, null);
    }

    public ExportArtifactsService(
            final ArtifactPersistencePort artifactPersistencePort,
            final AiArtifactPersistencePort aiArtifactPersistencePort) {
        this.artifactPersistencePort = Objects.requireNonNull(artifactPersistencePort);
        this.aiArtifactPersistencePort = aiArtifactPersistencePort;
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

        List<CodeArtifact> artifacts = resolveArtifactsForExport(sessionId);

        if (artifacts.isEmpty()) {
            return new ExportResult(targetDirectory, List.of(), List.of("Aucun artefact trouve pour la session " + sessionId));
        }

        for (CodeArtifact artifact : artifacts) {
            String fileName = artifact.className() + ".java";
            String content = artifact.content() != null ? artifact.content() : "// Contenu non genere\n";
            Path filePath = resolveFilePath(targetDir, artifact, fileName, content);
            try {
                Files.createDirectories(filePath.getParent());
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

    private List<CodeArtifact> resolveArtifactsForExport(final String sessionId) {
        List<CodeArtifact> templateArtifacts = artifactPersistencePort.findBySessionId(sessionId)
                .map(GenerationResult::artifacts)
                .orElse(List.of());
        List<AiGeneratedArtifact> aiArtifacts = (aiArtifactPersistencePort != null)
                ? aiArtifactPersistencePort.findLatestBySessionId(sessionId)
                : List.of();

        if (aiArtifacts.isEmpty()) {
            return templateArtifacts;
        }
        return mergeArtifacts(templateArtifacts, aiArtifacts);
    }

    private List<CodeArtifact> mergeArtifacts(
            final List<CodeArtifact> templateArtifacts,
            final List<AiGeneratedArtifact> aiArtifacts) {
        Map<ArtifactType, AiGeneratedArtifact> aiArtifactsByType = indexAiArtifactsByType(aiArtifacts);
        List<CodeArtifact> mergedArtifacts = new ArrayList<>();
        Set<ArtifactType> exportedTypes = new LinkedHashSet<>();

        for (CodeArtifact templateArtifact : templateArtifacts) {
            AiGeneratedArtifact aiArtifact = aiArtifactsByType.get(templateArtifact.type());
            CodeArtifact artifactToExport = (aiArtifact != null)
                    ? mergeArtifact(templateArtifact, aiArtifact)
                    : templateArtifact;
            mergedArtifacts.add(artifactToExport);
            exportedTypes.add(templateArtifact.type());
        }

        for (Map.Entry<ArtifactType, AiGeneratedArtifact> entry : aiArtifactsByType.entrySet()) {
            if (!exportedTypes.contains(entry.getKey())) {
                mergedArtifacts.add(toCodeArtifact(entry.getValue(), entry.getKey()));
            }
        }

        return List.copyOf(mergedArtifacts);
    }

    private Map<ArtifactType, AiGeneratedArtifact> indexAiArtifactsByType(final List<AiGeneratedArtifact> aiArtifacts) {
        Map<ArtifactType, AiGeneratedArtifact> indexedArtifacts = new LinkedHashMap<>();
        for (AiGeneratedArtifact aiArtifact : aiArtifacts) {
            ArtifactType artifactType = resolveArtifactType(aiArtifact.artifactType());
            if (artifactType != null && !AiArtifactImplementationInspector.isIncomplete(aiArtifact.content())) {
                indexedArtifacts.putIfAbsent(artifactType, aiArtifact);
            } else if (artifactType != null) {
                log.warn("Artefact IA ignore pendant l'export car incomplet - type={}, className={}",
                        aiArtifact.artifactType(),
                        aiArtifact.className());
            }
        }
        return indexedArtifacts;
    }

    private CodeArtifact mergeArtifact(
            final CodeArtifact templateArtifact,
            final AiGeneratedArtifact aiArtifact) {
        return new CodeArtifact(
                templateArtifact.artifactId(),
                templateArtifact.type(),
                templateArtifact.lotNumber(),
                aiArtifact.className(),
                normalizeContent(aiArtifact.content()),
                templateArtifact.transitionalBridge());
    }

    private CodeArtifact toCodeArtifact(
            final AiGeneratedArtifact aiArtifact,
            final ArtifactType artifactType) {
        return new CodeArtifact(
                "ai-" + artifactType.name().toLowerCase(),
                artifactType,
                resolveLotNumber(artifactType),
                aiArtifact.className(),
                normalizeContent(aiArtifact.content()),
                ArtifactType.BRIDGE == artifactType || ArtifactType.CONTROLLER_SLIM == artifactType);
    }

    private ArtifactType resolveArtifactType(final String rawArtifactType) {
        try {
            return ArtifactType.valueOf(rawArtifactType.trim().toUpperCase());
        } catch (RuntimeException exception) {
            log.warn("Type d'artefact IA ignore pendant l'export: {}", rawArtifactType);
            return null;
        }
    }

    private int resolveLotNumber(final ArtifactType artifactType) {
        int lotNumber = 5;
        if (ArtifactType.CONTROLLER_SLIM == artifactType || ArtifactType.BRIDGE == artifactType) {
            lotNumber = 1;
        } else if (ArtifactType.USE_CASE == artifactType
                || ArtifactType.VIEW_MODEL == artifactType
                || ArtifactType.TEST_SKELETON == artifactType) {
            lotNumber = 2;
        } else if (ArtifactType.POLICY == artifactType) {
            lotNumber = 3;
        } else if (ArtifactType.GATEWAY == artifactType) {
            lotNumber = 4;
        }
        return lotNumber;
    }

    private Path resolveFilePath(
            final Path targetDir,
            final CodeArtifact artifact,
            final String fileName,
            final String content) {
        String packagePath = extractPackagePath(content);
        Path sourceRoot = resolveSourceBaseDirectory(targetDir, artifact.type());
        Path packageDirectory = packagePath.isBlank() ? sourceRoot : sourceRoot.resolve(packagePath);
        return packageDirectory.resolve(fileName);
    }

    private Path resolveSourceBaseDirectory(
            final Path targetDir,
            final ArtifactType artifactType) {
        String normalizedTarget = normalizeSeparators(targetDir);
        boolean pointsToMainSources = normalizedTarget.contains(MAIN_SOURCE_ROOT);
        boolean pointsToTestSources = normalizedTarget.contains(TEST_SOURCE_ROOT);
        String sourceRoot = resolveSourceRoot(artifactType);
        Path sourceBaseDirectory = targetDir;
        if (!pointsToMainSources && !pointsToTestSources) {
            sourceBaseDirectory = targetDir.resolve(sourceRoot);
        } else if (TEST_SOURCE_ROOT.equals(sourceRoot) && pointsToMainSources) {
            sourceBaseDirectory = Path.of(normalizedTarget.replace(MAIN_SOURCE_ROOT, TEST_SOURCE_ROOT));
        } else if (MAIN_SOURCE_ROOT.equals(sourceRoot) && pointsToTestSources) {
            sourceBaseDirectory = Path.of(normalizedTarget.replace(TEST_SOURCE_ROOT, MAIN_SOURCE_ROOT));
        }
        return sourceBaseDirectory;
    }

    private String resolveSourceRoot(final ArtifactType artifactType) {
        String sourceRoot = MAIN_SOURCE_ROOT;
        if (ArtifactType.TEST_SKELETON == artifactType) {
            sourceRoot = TEST_SOURCE_ROOT;
        }
        return sourceRoot;
    }

    private String extractPackagePath(final String content) {
        String packagePath = "";
        Matcher matcher = PACKAGE_DECLARATION.matcher(content);
        if (matcher.find()) {
            packagePath = matcher.group(1).replace('.', '/');
        }
        return packagePath;
    }

    private String normalizeSeparators(final Path path) {
        return path.toString().replace('\\', '/');
    }

    private String normalizeContent(final String content) {
        return (content != null) ? content.replace("\r\n", "\n").replace("\r", "\n") : "";
    }
}
