package ff.ss.javaFxAuditStudio.application.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.in.GenerateSpringBootClassesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AiArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ProjectReferencePatternPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

/**
 * Service applicatif de génération IA des classes cibles Spring Boot (JAS-031 / IAP-2).
 *
 * <p>Assemble via {@code AiEnrichmentOrchestraConfiguration} — pas de {@code @Service}.
 */
public class AiSpringBootGenerationService implements GenerateSpringBootClassesUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(AiSpringBootGenerationService.class);
    private static final String PROMPT_TEMPLATE = "spring-boot-generation";

    private final AnalysisSessionPort sessionPort;
    private final ClassificationPersistencePort classificationPort;
    private final CartographyPersistencePort cartographyPort;
    private final ReclassificationAuditPort reclassificationAuditPort;
    private final AiArtifactPersistencePort aiArtifactPersistencePort;
    private final ProjectReferencePatternPort projectReferencePatternPort;
    private final AiEnrichmentPort aiEnrichmentPort;
    private final SanitizationPort sanitizationPort;
    private final SourceFileReaderPort sourceFileReaderPort;

    public AiSpringBootGenerationService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort) {
        this(sessionPort, classificationPort, null, null, null, null, aiEnrichmentPort, sanitizationPort, null);
    }

    public AiSpringBootGenerationService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SourceFileReaderPort sourceFileReaderPort) {
        this(sessionPort, classificationPort, null, null, null, null, aiEnrichmentPort, sanitizationPort, sourceFileReaderPort);
    }

    public AiSpringBootGenerationService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final CartographyPersistencePort cartographyPort,
            final ReclassificationAuditPort reclassificationAuditPort,
            final AiArtifactPersistencePort aiArtifactPersistencePort,
            final ProjectReferencePatternPort projectReferencePatternPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SourceFileReaderPort sourceFileReaderPort) {
        this.sessionPort = Objects.requireNonNull(sessionPort, "sessionPort must not be null");
        this.classificationPort = Objects.requireNonNull(classificationPort, "classificationPort must not be null");
        this.cartographyPort = cartographyPort;
        this.reclassificationAuditPort = reclassificationAuditPort;
        this.aiArtifactPersistencePort = aiArtifactPersistencePort;
        this.projectReferencePatternPort = projectReferencePatternPort;
        this.aiEnrichmentPort = Objects.requireNonNull(aiEnrichmentPort, "aiEnrichmentPort must not be null");
        this.sanitizationPort = sanitizationPort;
        this.sourceFileReaderPort = sourceFileReaderPort;
    }

    @Override
    public AiCodeGenerationResult generate(final String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");

        AnalysisSession session = sessionPort.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));

        ClassificationResult classification = classificationPort.findBySessionId(sessionId).orElse(null);

        String requestId = UUID.randomUUID().toString();

        if (classification == null) {
            LOG.warn("Génération IA : pas de classification pour session {}", sessionId);
            return AiCodeGenerationResult.degraded(requestId,
                    "Pas de classification disponible pour cette session. Lancez d'abord une analyse.");
        }

        String controllerRef = session.controllerName();
        String rawSource = LlmServiceSupport.readSourceFile(controllerRef, sourceFileReaderPort);
        String formattedRules = LlmServiceSupport.formatRules(classification);
        ControllerCartography cartography = loadCartography(sessionId);

        SanitizedBundle bundle;
        try {
            bundle = LlmServiceSupport.buildBundle(requestId, rawSource, controllerRef, sanitizationPort);
        } catch (SanitizationRefusedException e) {
            LOG.warn("Génération IA abandonnée pour session {} : {}", sessionId, e.getMessage());
            return AiCodeGenerationResult.degraded(requestId,
                    "Sanitisation refusée : " + e.getMessage());
        }

        AiEnrichmentRequest request = new AiEnrichmentRequest(
                requestId,
                bundle,
                TaskType.SPRING_BOOT_GENERATION,
                PROMPT_TEMPLATE,
                buildExtraContext(session, classification, formattedRules, cartography));

        AiEnrichmentResult llmResult = aiEnrichmentPort.enrich(request);

        return mapToGenerationResult(sessionId, llmResult);
    }

    private ControllerCartography loadCartography(final String sessionId) {
        if (cartographyPort == null) {
            return null;
        }
        java.util.Optional<ControllerCartography> cartography = cartographyPort.findBySessionId(sessionId);
        if (cartography == null) {
            return null;
        }
        return cartography.orElse(null);
    }

    private Map<String, Object> buildExtraContext(
            final AnalysisSession session,
            final ClassificationResult classification,
            final String formattedRules,
            final ControllerCartography cartography) {
        Map<String, Object> context = new java.util.HashMap<>();
        context.put("classifiedRules", formattedRules);
        context.put("screenContext", LlmServiceSupport.formatScreenContext(session, classification, cartography));
        context.put("reclassificationFeedback",
                LlmServiceSupport.formatReclassificationFeedback(
                        session.sessionId(), classification, reclassificationAuditPort));
        context.put("projectReferencePatterns", LlmServiceSupport.formatProjectReferencePatterns(loadProjectReferencePatterns()));
        return context;
    }

    private java.util.List<ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern> loadProjectReferencePatterns() {
        return (projectReferencePatternPort != null) ? projectReferencePatternPort.findAll() : java.util.List.of();
    }

    private AiCodeGenerationResult mapToGenerationResult(
            final String sessionId,
            final AiEnrichmentResult llmResult) {
        if (llmResult.degraded()) {
            return new AiCodeGenerationResult(
                    llmResult.requestId(),
                    true,
                    llmResult.degradationReason(),
                    Map.of(),
                    0,
                    llmResult.provider());
        }

        Map<String, String> cleanedClasses = sanitizeGeneratedArtifacts(llmResult.suggestions());
        if (cleanedClasses.isEmpty()) {
            LOG.warn("Generation IA {}: aucun artefact supporte retourne par le fournisseur {}", sessionId, llmResult.provider());
            return new AiCodeGenerationResult(
                    llmResult.requestId(),
                    true,
                    "Le fournisseur IA n'a retourne aucun artefact supporte.",
                    Map.of(),
                    llmResult.tokensUsed(),
                    llmResult.provider());
        }

        if (aiArtifactPersistencePort != null && !cleanedClasses.isEmpty()) {
            aiArtifactPersistencePort.saveGeneratedArtifacts(
                    sessionId,
                    llmResult.requestId(),
                    llmResult.provider(),
                    TaskType.SPRING_BOOT_GENERATION,
                    cleanedClasses);
        }

        return new AiCodeGenerationResult(
                llmResult.requestId(),
                false,
                "",
                cleanedClasses,
                llmResult.tokensUsed(),
                llmResult.provider());
    }

    private Map<String, String> sanitizeGeneratedArtifacts(final Map<String, String> suggestions) {
        Map<String, String> cleaned = new LinkedHashMap<>();
        suggestions.forEach((key, value) -> {
            ArtifactType artifactType = resolveArtifactType(key);
            String normalizedContent = normalizeContent(value);
            if (artifactType == null || normalizedContent.isBlank()) {
                LOG.debug("Artefact IA ignore car non supporte ou vide: {}", key);
                return;
            }
            cleaned.put(artifactType.name(), normalizedContent);
        });
        return Map.copyOf(cleaned);
    }

    private ArtifactType resolveArtifactType(final String rawArtifactType) {
        ArtifactType artifactType = null;
        if (rawArtifactType != null) {
            try {
                artifactType = ArtifactType.valueOf(rawArtifactType.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                artifactType = null;
            }
        }
        return artifactType;
    }

    private String normalizeContent(final String content) {
        return (content != null) ? content.replace("\r\n", "\n").replace("\r", "\n") : "";
    }
}
