package ff.ss.javaFxAuditStudio.application.service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.in.RefineArtifactUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.PromptContextSanitizerPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.ArtifactRefineRequest;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

/**
 * Service applicatif de raffinement d'un artefact Spring Boot genere.
 */
public class RefineArtifactService implements RefineArtifactUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RefineArtifactService.class);
    private static final String PROMPT_TEMPLATE = "artifact-refine";

    private final AnalysisSessionPort sessionPort;
    private final ClassificationPersistencePort classificationPort;
    private final CartographyPersistencePort cartographyPort;
    private final ReclassificationAuditPort reclassificationAuditPort;
    private final AiEnrichmentPort aiEnrichmentPort;
    private final SanitizationPort sanitizationPort;
    private final SourceFileReaderPort sourceFileReaderPort;
    private final PromptContextSanitizerPort promptContextSanitizerPort;

    public RefineArtifactService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort) {
        this(sessionPort, classificationPort, null, null, aiEnrichmentPort, sanitizationPort, null, null);
    }

    public RefineArtifactService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final CartographyPersistencePort cartographyPort,
            final ReclassificationAuditPort reclassificationAuditPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SourceFileReaderPort sourceFileReaderPort) {
        this(sessionPort, classificationPort, cartographyPort, reclassificationAuditPort,
                aiEnrichmentPort, sanitizationPort, sourceFileReaderPort, null);
    }

    public RefineArtifactService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final CartographyPersistencePort cartographyPort,
            final ReclassificationAuditPort reclassificationAuditPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SourceFileReaderPort sourceFileReaderPort,
            final PromptContextSanitizerPort promptContextSanitizerPort) {
        this.sessionPort = Objects.requireNonNull(sessionPort, "sessionPort must not be null");
        this.classificationPort = Objects.requireNonNull(classificationPort, "classificationPort must not be null");
        this.cartographyPort = cartographyPort;
        this.reclassificationAuditPort = reclassificationAuditPort;
        this.aiEnrichmentPort = Objects.requireNonNull(aiEnrichmentPort, "aiEnrichmentPort must not be null");
        this.sanitizationPort = sanitizationPort;
        this.sourceFileReaderPort = sourceFileReaderPort;
        this.promptContextSanitizerPort = promptContextSanitizerPort;
    }

    @Override
    public AiCodeGenerationResult refine(final String sessionId, final ArtifactRefineRequest request) {
        return refine(sessionId, request, null);
    }

    @Override
    public AiCodeGenerationResult refine(
            final String sessionId,
            final ArtifactRefineRequest request,
            final LlmProvider provider) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        AnalysisSession session = sessionPort.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
        ClassificationResult classification = classificationPort.findBySessionId(sessionId).orElse(null);

        String requestId = UUID.randomUUID().toString();
        if (classification == null) {
            LOG.warn("Raffinement IA : pas de classification pour session {}", sessionId);
            return AiCodeGenerationResult.degraded(requestId,
                    "Pas de classification disponible pour cette session. Lancez d'abord une analyse.");
        }

        String controllerRef = session.controllerName();
        String rawSource = LlmServiceSupport.readSourceFile(controllerRef, sourceFileReaderPort);
        ControllerCartography cartography = loadCartography(sessionId);

        SanitizedBundle bundle;
        try {
            bundle = LlmServiceSupport.buildBundle(requestId, rawSource, controllerRef, sanitizationPort);
        } catch (SanitizationRefusedException e) {
            LOG.warn("Raffinement IA abandonne pour session {} : {}", sessionId, e.getMessage());
            return AiCodeGenerationResult.degraded(requestId, "Sanitisation refusee : " + e.getMessage());
        }

        AiEnrichmentRequest enrichmentRequest = new AiEnrichmentRequest(
                requestId,
                bundle,
                TaskType.SPRING_BOOT_GENERATION,
                PROMPT_TEMPLATE,
                buildExtraContext(requestId, session, classification, request, cartography));

        AiEnrichmentResult result = aiEnrichmentPort.enrich(enrichmentRequest, provider);
        return mapToGenerationResult(result);
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
            final String requestId,
            final AnalysisSession session,
            final ClassificationResult classification,
            final ArtifactRefineRequest request,
            final ControllerCartography cartography) {
        Map<String, Object> context = new java.util.HashMap<>();
        context.put("artifactType", request.artifactType().name());
        context.put("instruction", promptContextSanitizerPort != null
                ? promptContextSanitizerPort.sanitizeInstruction(
                        requestId, TaskType.SPRING_BOOT_GENERATION, request.instruction(), 2000)
                : request.instruction());
        context.put("previousCode", promptContextSanitizerPort != null
                ? promptContextSanitizerPort.sanitizeCodeFragment(
                        requestId, TaskType.SPRING_BOOT_GENERATION, request.previousCode(), "previousCode")
                : request.previousCode());
        context.put("classifiedRules", LlmServiceSupport.formatRules(classification));
        context.put("screenContext", LlmServiceSupport.formatScreenContext(session, classification, cartography));
        context.put("reclassificationFeedback",
                LlmServiceSupport.formatReclassificationFeedback(
                        session.sessionId(), classification, reclassificationAuditPort));
        return context;
    }

    private AiCodeGenerationResult mapToGenerationResult(final AiEnrichmentResult result) {
        if (result.degraded()) {
            return new AiCodeGenerationResult(
                    result.requestId(),
                    true,
                    result.degradationReason(),
                    Map.of(),
                    0,
                    result.provider());
        }

        Map<String, String> cleanedClasses = result.suggestions().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().replace("\r\n", "\n").replace("\r", "\n")));

        return new AiCodeGenerationResult(
                result.requestId(),
                false,
                "",
                cleanedClasses,
                result.tokensUsed(),
                result.provider());
    }
}
