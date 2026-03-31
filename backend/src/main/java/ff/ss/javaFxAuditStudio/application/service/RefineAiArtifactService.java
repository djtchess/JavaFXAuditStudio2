package ff.ss.javaFxAuditStudio.application.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.in.RefineAiArtifactUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AiArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ProjectReferencePatternPort;
import ff.ss.javaFxAuditStudio.application.ports.out.PromptContextSanitizerPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiArtifactRefinementCommand;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

/**
 * Raffinement multi-tour d'un artefact généré par l'IA.
 */
public class RefineAiArtifactService implements RefineAiArtifactUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RefineAiArtifactService.class);
    private static final String PROMPT_TEMPLATE = "artifact-refine";

    private final AnalysisSessionPort sessionPort;
    private final ClassificationPersistencePort classificationPort;
    private final CartographyPersistencePort cartographyPort;
    private final ReclassificationAuditPort reclassificationAuditPort;
    private final AiArtifactPersistencePort aiArtifactPersistencePort;
    private final ProjectReferencePatternPort projectReferencePatternPort;
    private final AiEnrichmentPort aiEnrichmentPort;
    private final SanitizationPort sanitizationPort;
    private final SourceFileReaderPort sourceFileReaderPort;
    private final PromptContextSanitizerPort promptContextSanitizerPort;

    public RefineAiArtifactService(
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
                aiArtifactPersistencePort, projectReferencePatternPort, aiEnrichmentPort,
                sanitizationPort, sourceFileReaderPort, null);
    }

    public RefineAiArtifactService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final CartographyPersistencePort cartographyPort,
            final ReclassificationAuditPort reclassificationAuditPort,
            final AiArtifactPersistencePort aiArtifactPersistencePort,
            final ProjectReferencePatternPort projectReferencePatternPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SourceFileReaderPort sourceFileReaderPort,
            final PromptContextSanitizerPort promptContextSanitizerPort) {
        this.sessionPort = Objects.requireNonNull(sessionPort, "sessionPort must not be null");
        this.classificationPort = Objects.requireNonNull(classificationPort, "classificationPort must not be null");
        this.cartographyPort = cartographyPort;
        this.reclassificationAuditPort = reclassificationAuditPort;
        this.aiArtifactPersistencePort = Objects.requireNonNull(
                aiArtifactPersistencePort,
                "aiArtifactPersistencePort must not be null");
        this.projectReferencePatternPort = projectReferencePatternPort;
        this.aiEnrichmentPort = Objects.requireNonNull(aiEnrichmentPort, "aiEnrichmentPort must not be null");
        this.sanitizationPort = sanitizationPort;
        this.sourceFileReaderPort = sourceFileReaderPort;
        this.promptContextSanitizerPort = promptContextSanitizerPort;
    }

    @Override
    public AiCodeGenerationResult refine(final String sessionId, final AiArtifactRefinementCommand command) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(command, "command must not be null");

        AnalysisSession session = sessionPort.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
        ClassificationResult classification = classificationPort.findBySessionId(sessionId).orElse(null);
        String requestId = UUID.randomUUID().toString();
        if (classification == null) {
            return AiCodeGenerationResult.degraded(
                    requestId,
                    "Pas de classification disponible pour cette session. Lancez d'abord une analyse.");
        }

        String controllerRef = session.controllerName();
        String rawSource = LlmServiceSupport.readSourceFile(controllerRef, sourceFileReaderPort);
        SanitizedBundle bundle = buildBundle(requestId, rawSource, controllerRef, sessionId);
        if (bundle == null) {
            return AiCodeGenerationResult.degraded(requestId, "Sanitisation refusee pour ce raffinement.");
        }

        ControllerCartography cartography = loadCartography(sessionId);
        String previousCode = aiArtifactPersistencePort
                .findLatestBySessionIdAndArtifactType(sessionId, command.artifactType())
                .map(artifact -> artifact.content())
                .filter(content -> !content.isBlank())
                .orElse(command.previousCode());

        if (previousCode.isBlank()) {
            return AiCodeGenerationResult.degraded(
                    requestId,
                    "Aucun contenu precedent disponible pour l'artefact " + command.artifactType());
        }

        AiEnrichmentResult llmResult = aiEnrichmentPort.enrich(new AiEnrichmentRequest(
                requestId,
                bundle,
                TaskType.ARTIFACT_REFINEMENT,
                PROMPT_TEMPLATE,
                buildExtraContext(requestId, session, classification, cartography, command, previousCode)));

        return mapToRefinementResult(sessionId, command, llmResult);
    }

    private SanitizedBundle buildBundle(
            final String requestId,
            final String rawSource,
            final String controllerRef,
            final String sessionId) {
        SanitizedBundle bundle = null;
        try {
            bundle = LlmServiceSupport.buildBundle(requestId, rawSource, controllerRef, sanitizationPort);
        } catch (SanitizationRefusedException exception) {
            LOG.warn("Raffinement IA abandonne pour session {} : {}", sessionId, exception.getMessage());
        }
        return bundle;
    }

    private ControllerCartography loadCartography(final String sessionId) {
        return (cartographyPort != null) ? cartographyPort.findBySessionId(sessionId).orElse(null) : null;
    }

    private Map<String, Object> buildExtraContext(
            final String requestId,
            final AnalysisSession session,
            final ClassificationResult classification,
            final ControllerCartography cartography,
            final AiArtifactRefinementCommand command,
            final String previousCode) {
        Map<String, Object> context = new HashMap<>();
        context.put("artifactType", command.artifactType());
        context.put("refineInstruction", promptContextSanitizerPort != null
                ? promptContextSanitizerPort.sanitizeInstruction(command.instruction(), 2000)
                : command.instruction());
        context.put("currentArtifactCode", promptContextSanitizerPort != null
                ? promptContextSanitizerPort.sanitizeCodeFragment(requestId, previousCode, "currentArtifactCode")
                : previousCode);
        context.put("classifiedRules", LlmServiceSupport.formatRules(classification));
        context.put("screenContext", LlmServiceSupport.formatScreenContext(session, classification, cartography));
        context.put(
                "reclassificationFeedback",
                LlmServiceSupport.formatReclassificationFeedback(
                        session.sessionId(),
                        classification,
                        reclassificationAuditPort));
        context.put(
                "projectReferencePatterns",
                promptContextSanitizerPort != null
                        ? promptContextSanitizerPort.sanitizeReferencePatterns(
                                requestId, loadProjectReferencePatterns(command.artifactType()))
                        : LlmServiceSupport.formatProjectReferencePatterns(
                                loadProjectReferencePatterns(command.artifactType())));
        return context;
    }

    private java.util.List<ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern> loadProjectReferencePatterns(
            final String artifactType) {
        return (projectReferencePatternPort != null)
                ? projectReferencePatternPort.findByArtifactType(artifactType)
                : java.util.List.of();
    }

    private AiCodeGenerationResult mapToRefinementResult(
            final String sessionId,
            final AiArtifactRefinementCommand command,
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

        String refinedCode = extractRefinedCode(command.artifactType(), llmResult);
        if (refinedCode.isBlank()) {
            return AiCodeGenerationResult.degraded(
                    llmResult.requestId(),
                    "Le fournisseur IA n'a retourne aucun code pour " + command.artifactType());
        }

        aiArtifactPersistencePort.saveArtifactVersion(
                sessionId,
                command.artifactType(),
                refinedCode,
                llmResult.requestId(),
                llmResult.provider(),
                TaskType.ARTIFACT_REFINEMENT);

        return new AiCodeGenerationResult(
                llmResult.requestId(),
                false,
                "",
                Map.of(command.artifactType(), refinedCode),
                llmResult.tokensUsed(),
                llmResult.provider());
    }

    private String extractRefinedCode(final String artifactType, final AiEnrichmentResult llmResult) {
        String refinedCode = llmResult.suggestions().get(artifactType);
        if (refinedCode == null || refinedCode.isBlank()) {
            refinedCode = llmResult.suggestions().get("artifact");
        }
        if ((refinedCode == null || refinedCode.isBlank()) && !llmResult.suggestions().isEmpty()) {
            refinedCode = llmResult.suggestions().values().iterator().next();
        }
        return (refinedCode != null) ? refinedCode.replace("\r\n", "\n").replace("\r", "\n") : "";
    }
}
