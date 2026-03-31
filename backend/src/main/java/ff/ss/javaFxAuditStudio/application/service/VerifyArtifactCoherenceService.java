package ff.ss.javaFxAuditStudio.application.service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.in.VerifyArtifactCoherenceUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.ArtifactCoherenceResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

/**
 * Service applicatif de verification de coherence inter-artefacts.
 */
public class VerifyArtifactCoherenceService implements VerifyArtifactCoherenceUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(VerifyArtifactCoherenceService.class);
    private static final String PROMPT_TEMPLATE = "artifact-coherence";

    private final AnalysisSessionPort sessionPort;
    private final ClassificationPersistencePort classificationPort;
    private final CartographyPersistencePort cartographyPort;
    private final ArtifactPersistencePort artifactPersistencePort;
    private final ReclassificationAuditPort reclassificationAuditPort;
    private final AiEnrichmentPort aiEnrichmentPort;
    private final SanitizationPort sanitizationPort;
    private final SourceFileReaderPort sourceFileReaderPort;

    public VerifyArtifactCoherenceService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final ArtifactPersistencePort artifactPersistencePort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort) {
        this(sessionPort, classificationPort, null, artifactPersistencePort, null, aiEnrichmentPort, sanitizationPort, null);
    }

    public VerifyArtifactCoherenceService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final CartographyPersistencePort cartographyPort,
            final ArtifactPersistencePort artifactPersistencePort,
            final ReclassificationAuditPort reclassificationAuditPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SourceFileReaderPort sourceFileReaderPort) {
        this.sessionPort = Objects.requireNonNull(sessionPort, "sessionPort must not be null");
        this.classificationPort = Objects.requireNonNull(classificationPort, "classificationPort must not be null");
        this.cartographyPort = cartographyPort;
        this.artifactPersistencePort = Objects.requireNonNull(artifactPersistencePort, "artifactPersistencePort must not be null");
        this.reclassificationAuditPort = reclassificationAuditPort;
        this.aiEnrichmentPort = Objects.requireNonNull(aiEnrichmentPort, "aiEnrichmentPort must not be null");
        this.sanitizationPort = sanitizationPort;
        this.sourceFileReaderPort = sourceFileReaderPort;
    }

    @Override
    public ArtifactCoherenceResult verify(final String sessionId) {
        return verify(sessionId, null);
    }

    @Override
    public ArtifactCoherenceResult verify(final String sessionId, final LlmProvider provider) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");

        AnalysisSession session = sessionPort.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
        ClassificationResult classification = classificationPort.findBySessionId(sessionId).orElse(null);
        GenerationResult generationResult = artifactPersistencePort.findBySessionId(sessionId).orElse(null);

        String requestId = UUID.randomUUID().toString();
        if (classification == null) {
            LOG.warn("Coherence IA : pas de classification pour session {}", sessionId);
            return ArtifactCoherenceResult.degraded(requestId,
                    "Pas de classification disponible pour cette session. Lancez d'abord une analyse.");
        }
        if (generationResult == null || generationResult.artifacts().isEmpty()) {
            LOG.warn("Coherence IA : pas d'artefacts generes pour session {}", sessionId);
            return ArtifactCoherenceResult.degraded(requestId,
                    "Aucun artefact genere disponible pour verifier la coherence.");
        }

        String controllerRef = session.controllerName();
        String rawSource = LlmServiceSupport.readSourceFile(controllerRef, sourceFileReaderPort);
        ControllerCartography cartography = loadCartography(sessionId);

        SanitizedBundle bundle;
        try {
            bundle = LlmServiceSupport.buildBundle(requestId, rawSource, controllerRef, sanitizationPort);
        } catch (SanitizationRefusedException e) {
            LOG.warn("Coherence IA abandonnee pour session {} : {}", sessionId, e.getMessage());
            return ArtifactCoherenceResult.degraded(requestId, "Sanitisation refusee : " + e.getMessage());
        }

        AiEnrichmentRequest enrichmentRequest = new AiEnrichmentRequest(
                requestId,
                bundle,
                TaskType.ARTIFACT_REVIEW,
                PROMPT_TEMPLATE,
                buildExtraContext(session, classification, generationResult, cartography));

        AiEnrichmentResult result = provider == null
                ? aiEnrichmentPort.enrich(enrichmentRequest)
                : aiEnrichmentPort.enrich(enrichmentRequest, provider);
        return mapToCoherenceResult(result);
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
            final GenerationResult generationResult,
            final ControllerCartography cartography) {
        Map<String, Object> context = new java.util.HashMap<>();
        context.put("classifiedRules", LlmServiceSupport.formatRules(classification));
        context.put("screenContext", LlmServiceSupport.formatScreenContext(session, classification, cartography));
        context.put("reclassificationFeedback",
                LlmServiceSupport.formatReclassificationFeedback(
                        session.sessionId(), classification, reclassificationAuditPort));
        context.put("generatedArtifacts", LlmServiceSupport.formatGeneratedArtifacts(generationResult));
        return context;
    }

    private ArtifactCoherenceResult mapToCoherenceResult(final AiEnrichmentResult result) {
        if (result.degraded()) {
            return ArtifactCoherenceResult.degraded(result.requestId(), result.degradationReason());
        }

        boolean coherent = true;
        Map<String, String> issues = new java.util.LinkedHashMap<>();
        java.util.List<String> globalSuggestions = new java.util.ArrayList<>();

        for (Map.Entry<String, String> entry : result.suggestions().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().replace("\r\n", "\n").replace("\r", "\n");
            if ("coherent".equalsIgnoreCase(key)) {
                coherent = Boolean.parseBoolean(value.trim());
            } else if ("global".equalsIgnoreCase(key)) {
                globalSuggestions = java.util.Arrays.stream(value.split("\n"))
                        .filter(line -> !line.isBlank())
                        .toList();
            } else {
                issues.put(normalizeArtifactIssueKey(key), value);
            }
        }

        if (result.suggestions().containsKey("coherent") && !coherent) {
            coherent = false;
        } else if (!result.suggestions().containsKey("coherent")) {
            coherent = issues.isEmpty();
        }

        return new ArtifactCoherenceResult(
                result.requestId(),
                false,
                "",
                coherent,
                Map.copyOf(issues),
                globalSuggestions,
                result.provider());
    }

    private String normalizeArtifactIssueKey(final String key) {
        if (key.startsWith("artifact_")) {
            return key.substring("artifact_".length());
        }
        return key;
    }
}
