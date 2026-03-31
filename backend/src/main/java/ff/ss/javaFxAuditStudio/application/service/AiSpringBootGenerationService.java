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
import ff.ss.javaFxAuditStudio.application.ports.out.MigrationPlanPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ProjectReferencePatternPort;
import ff.ss.javaFxAuditStudio.application.ports.out.PromptContextSanitizerPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;
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
    private final MigrationPlanPersistencePort migrationPlanPersistencePort;
    private final AiEnrichmentPort aiEnrichmentPort;
    private final SanitizationPort sanitizationPort;
    private final SourceFileReaderPort sourceFileReaderPort;
    private final PromptContextSanitizerPort promptContextSanitizerPort;

    public AiSpringBootGenerationService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort) {
        this(sessionPort, classificationPort, null, null, null, null, null, aiEnrichmentPort, sanitizationPort, null, null);
    }

    public AiSpringBootGenerationService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SourceFileReaderPort sourceFileReaderPort) {
        this(sessionPort, classificationPort, null, null, null, null, null, aiEnrichmentPort, sanitizationPort, sourceFileReaderPort, null);
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
        this(sessionPort, classificationPort, cartographyPort, reclassificationAuditPort,
                aiArtifactPersistencePort, projectReferencePatternPort, null,
                aiEnrichmentPort, sanitizationPort, sourceFileReaderPort, null);
    }

    public AiSpringBootGenerationService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final CartographyPersistencePort cartographyPort,
            final ReclassificationAuditPort reclassificationAuditPort,
            final AiArtifactPersistencePort aiArtifactPersistencePort,
            final ProjectReferencePatternPort projectReferencePatternPort,
            final MigrationPlanPersistencePort migrationPlanPersistencePort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SourceFileReaderPort sourceFileReaderPort) {
        this(sessionPort, classificationPort, cartographyPort, reclassificationAuditPort,
                aiArtifactPersistencePort, projectReferencePatternPort, migrationPlanPersistencePort,
                aiEnrichmentPort, sanitizationPort, sourceFileReaderPort, null);
    }

    public AiSpringBootGenerationService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final CartographyPersistencePort cartographyPort,
            final ReclassificationAuditPort reclassificationAuditPort,
            final AiArtifactPersistencePort aiArtifactPersistencePort,
            final ProjectReferencePatternPort projectReferencePatternPort,
            final MigrationPlanPersistencePort migrationPlanPersistencePort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SourceFileReaderPort sourceFileReaderPort,
            final PromptContextSanitizerPort promptContextSanitizerPort) {
        this.sessionPort = Objects.requireNonNull(sessionPort, "sessionPort must not be null");
        this.classificationPort = Objects.requireNonNull(classificationPort, "classificationPort must not be null");
        this.cartographyPort = cartographyPort;
        this.reclassificationAuditPort = reclassificationAuditPort;
        this.aiArtifactPersistencePort = aiArtifactPersistencePort;
        this.projectReferencePatternPort = projectReferencePatternPort;
        this.migrationPlanPersistencePort = migrationPlanPersistencePort;
        this.aiEnrichmentPort = Objects.requireNonNull(aiEnrichmentPort, "aiEnrichmentPort must not be null");
        this.sanitizationPort = sanitizationPort;
        this.sourceFileReaderPort = sourceFileReaderPort;
        this.promptContextSanitizerPort = promptContextSanitizerPort;
    }

    @Override
    public AiCodeGenerationResult generate(final String sessionId) {
        return generate(sessionId, null);
    }

    @Override
    public AiCodeGenerationResult generate(final String sessionId, final LlmProvider provider) {
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
        MigrationPlan migrationPlan = loadMigrationPlan(sessionId);

        SanitizedBundle bundle;
        try {
            bundle = LlmServiceSupport.buildBundle(requestId, rawSource, controllerRef, sanitizationPort);
        } catch (SanitizationRefusedException e) {
            LOG.warn("Génération IA abandonnée pour session {} : {}", sessionId, e.getMessage());
            return AiCodeGenerationResult.degraded(requestId,
                    "Sanitisation refusée : " + e.getMessage());
        }

        String ruleSourceSnippets = LlmServiceSupport.formatRuleSourceSnippets(bundle.sanitizedSource(), classification);
        if (promptContextSanitizerPort != null) {
            ruleSourceSnippets = promptContextSanitizerPort.sanitizeCodeFragment(
                    requestId,
                    TaskType.SPRING_BOOT_GENERATION,
                    ruleSourceSnippets,
                    "ruleSourceSnippets");
        }
        AiEnrichmentRequest request = new AiEnrichmentRequest(
                requestId,
                bundle,
                TaskType.SPRING_BOOT_GENERATION,
                PROMPT_TEMPLATE,
                buildExtraContext(
                        requestId,
                        session,
                        classification,
                        formattedRules,
                        cartography,
                        migrationPlan,
                        ruleSourceSnippets));

        AiEnrichmentResult llmResult = aiEnrichmentPort.enrich(request, provider);

        return mapToGenerationResult(sessionId, llmResult, ruleSourceSnippets);
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

    private MigrationPlan loadMigrationPlan(final String sessionId) {
        if (migrationPlanPersistencePort == null) {
            return null;
        }
        return migrationPlanPersistencePort.findBySessionId(sessionId).orElse(null);
    }

    private Map<String, Object> buildExtraContext(
            final String requestId,
            final AnalysisSession session,
            final ClassificationResult classification,
            final String formattedRules,
            final ControllerCartography cartography,
            final MigrationPlan migrationPlan,
            final String ruleSourceSnippets) {
        Map<String, Object> context = new java.util.HashMap<>();
        context.put("classifiedRules", formattedRules);
        context.put("screenContext", LlmServiceSupport.formatScreenContext(session, classification, cartography));
        context.put("migrationPlan", LlmServiceSupport.formatMigrationPlan(migrationPlan));
        context.put("ruleSourceSnippets", ruleSourceSnippets);
        context.put("reclassificationFeedback",
                LlmServiceSupport.formatReclassificationFeedback(
                        session.sessionId(), classification, reclassificationAuditPort));
        context.put("projectReferencePatterns", promptContextSanitizerPort != null
                ? promptContextSanitizerPort.sanitizeReferencePatterns(
                        requestId, TaskType.SPRING_BOOT_GENERATION, loadProjectReferencePatterns())
                : LlmServiceSupport.formatProjectReferencePatterns(loadProjectReferencePatterns()));
        return context;
    }

    private java.util.List<ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern> loadProjectReferencePatterns() {
        return (projectReferencePatternPort != null) ? projectReferencePatternPort.findAll() : java.util.List.of();
    }

    private AiCodeGenerationResult mapToGenerationResult(
            final String sessionId,
            final AiEnrichmentResult llmResult,
            final String ruleSourceSnippets) {
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

        if (hasActionableSourceSnippets(ruleSourceSnippets) && containsImplementationTodo(cleanedClasses)) {
            LOG.warn("Generation IA {}: des TODO persistent alors que des extraits source etaient disponibles", sessionId);
            return new AiCodeGenerationResult(
                    llmResult.requestId(),
                    true,
                    "Le fournisseur IA a retourne un code incomplet avec des TODO alors que des extraits source exploitables etaient disponibles.",
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

    private boolean hasActionableSourceSnippets(final String ruleSourceSnippets) {
        return ruleSourceSnippets != null
                && !ruleSourceSnippets.isBlank()
                && !ruleSourceSnippets.startsWith("Aucun extrait");
    }

    private boolean containsImplementationTodo(final Map<String, String> generatedArtifacts) {
        return generatedArtifacts.values().stream()
                .anyMatch(content -> content != null && content.contains("// TODO: implementer"));
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
