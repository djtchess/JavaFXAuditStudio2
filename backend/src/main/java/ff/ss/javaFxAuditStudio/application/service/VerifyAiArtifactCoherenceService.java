package ff.ss.javaFxAuditStudio.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.in.VerifyAiArtifactCoherenceUseCase;
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
import ff.ss.javaFxAuditStudio.domain.ai.AiArtifactCoherenceResult;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.AiGeneratedArtifact;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

/**
 * Vérification de cohérence inter-artefacts sur les versions IA persistées.
 */
public class VerifyAiArtifactCoherenceService implements VerifyAiArtifactCoherenceUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(VerifyAiArtifactCoherenceService.class);
    private static final String PROMPT_TEMPLATE = "artifact-coherence";

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

    public VerifyAiArtifactCoherenceService(
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

    public VerifyAiArtifactCoherenceService(
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
    public AiArtifactCoherenceResult verify(final String sessionId) {
        return verify(sessionId, null);
    }

    @Override
    public AiArtifactCoherenceResult verify(final String sessionId, final LlmProvider provider) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");

        AnalysisSession session = sessionPort.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
        ClassificationResult classification = classificationPort.findBySessionId(sessionId).orElse(null);
        String requestId = UUID.randomUUID().toString();
        if (classification == null) {
            return AiArtifactCoherenceResult.degraded(
                    requestId,
                    "Pas de classification disponible pour cette session. Lancez d'abord une analyse.");
        }

        List<AiGeneratedArtifact> artifacts = aiArtifactPersistencePort.findLatestBySessionId(sessionId);
        if (artifacts.isEmpty()) {
            return AiArtifactCoherenceResult.degraded(
                    requestId,
                    "Aucun artefact IA persiste pour cette session.");
        }

        SanitizedBundle bundle = buildBundle(requestId, session, sessionId);
        if (bundle == null) {
            return AiArtifactCoherenceResult.degraded(requestId, "Sanitisation refusee pour cette verification.");
        }

        ControllerCartography cartography = (cartographyPort != null)
                ? cartographyPort.findBySessionId(sessionId).orElse(null)
                : null;
        AiEnrichmentRequest request = new AiEnrichmentRequest(
                requestId,
                bundle,
                TaskType.ARTIFACT_COHERENCE,
                PROMPT_TEMPLATE,
                buildExtraContext(requestId, session, classification, cartography, artifacts));
        AiEnrichmentResult llmResult = provider == null
                ? aiEnrichmentPort.enrich(request)
                : aiEnrichmentPort.enrich(request, provider);

        return mapToCoherenceResult(llmResult);
    }

    private SanitizedBundle buildBundle(
            final String requestId,
            final AnalysisSession session,
            final String sessionId) {
        SanitizedBundle bundle = null;
        try {
            String rawSource = LlmServiceSupport.readSourceFile(session.controllerName(), sourceFileReaderPort);
            bundle = LlmServiceSupport.buildBundle(requestId, rawSource, session.controllerName(), sanitizationPort);
        } catch (SanitizationRefusedException exception) {
            LOG.warn("Verification de coherence abandonnee pour session {} : {}", sessionId, exception.getMessage());
        }
        return bundle;
    }

    private Map<String, Object> buildExtraContext(
            final String requestId,
            final AnalysisSession session,
            final ClassificationResult classification,
            final ControllerCartography cartography,
            final List<AiGeneratedArtifact> artifacts) {
        Map<String, Object> context = new HashMap<>();
        context.put("classifiedRules", LlmServiceSupport.formatRules(classification));
        context.put("screenContext", LlmServiceSupport.formatScreenContext(session, classification, cartography));
        context.put("generatedArtifacts", LlmServiceSupport.formatGeneratedArtifacts(artifacts));
        context.put("generatedArtifactDetails", promptContextSanitizerPort != null
                ? promptContextSanitizerPort.sanitizeArtifactDetails(
                        requestId, TaskType.ARTIFACT_COHERENCE, artifacts)
                : LlmServiceSupport.formatGeneratedArtifactDetails(artifacts));
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
                                requestId, TaskType.ARTIFACT_COHERENCE, loadProjectReferencePatterns(artifacts))
                        : LlmServiceSupport.formatProjectReferencePatterns(loadProjectReferencePatterns(artifacts)));
        return context;
    }

    private List<ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern> loadProjectReferencePatterns(
            final List<AiGeneratedArtifact> artifacts) {
        if (projectReferencePatternPort == null) {
            return List.of();
        }
        List<ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern> patterns = new java.util.ArrayList<>();
        artifacts.stream()
                .map(AiGeneratedArtifact::artifactType)
                .distinct()
                .forEach(artifactType -> patterns.addAll(projectReferencePatternPort.findByArtifactType(artifactType)));
        return List.copyOf(patterns);
    }

    private AiArtifactCoherenceResult mapToCoherenceResult(final AiEnrichmentResult llmResult) {
        if (llmResult.degraded()) {
            return new AiArtifactCoherenceResult(
                    llmResult.requestId(),
                    true,
                    llmResult.degradationReason(),
                    "",
                    Map.of(),
                    List.of(),
                    0,
                    llmResult.provider());
        }

        String summary = llmResult.suggestions().getOrDefault(
                "summary",
                llmResult.suggestions().getOrDefault("global", ""));
        Map<String, String> artifactFindings = new HashMap<>();
        for (Map.Entry<String, String> entry : llmResult.suggestions().entrySet()) {
            if (!"summary".equals(entry.getKey()) && !"global".equals(entry.getKey())) {
                artifactFindings.put(entry.getKey(), entry.getValue());
            }
        }

        List<String> globalFindings = llmResult.suggestions().containsKey("global")
                ? List.of(llmResult.suggestions().get("global").split("\\R"))
                : List.of();

        return new AiArtifactCoherenceResult(
                llmResult.requestId(),
                false,
                "",
                summary,
                Map.copyOf(artifactFindings),
                globalFindings.stream().filter(value -> !value.isBlank()).toList(),
                llmResult.tokensUsed(),
                llmResult.provider());
    }
}
