package ff.ss.javaFxAuditStudio.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.in.ReviewArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.ArtifactReviewResult;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

public class ReviewArtifactsService implements ReviewArtifactsUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewArtifactsService.class);
    private static final String PROMPT_TEMPLATE = "artifact-review";

    private final AnalysisSessionPort sessionPort;
    private final ClassificationPersistencePort classificationPort;
    private final AiEnrichmentPort aiEnrichmentPort;
    private final SanitizationPort sanitizationPort;
    private final SourceFileReaderPort sourceFileReaderPort;

    public ReviewArtifactsService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort) {
        this(sessionPort, classificationPort, aiEnrichmentPort, sanitizationPort, null);
    }

    public ReviewArtifactsService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SourceFileReaderPort sourceFileReaderPort) {
        this.sessionPort = Objects.requireNonNull(sessionPort);
        this.classificationPort = Objects.requireNonNull(classificationPort);
        this.aiEnrichmentPort = Objects.requireNonNull(aiEnrichmentPort);
        this.sanitizationPort = sanitizationPort;
        this.sourceFileReaderPort = sourceFileReaderPort;
    }

    @Override
    public ArtifactReviewResult review(final String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");

        AnalysisSession session = sessionPort.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));

        ClassificationResult classification = classificationPort.findBySessionId(sessionId).orElse(null);

        String requestId = UUID.randomUUID().toString();

        if (classification == null) {
            LOG.warn("Revue IA : pas de classification pour session {}", sessionId);
            return ArtifactReviewResult.degraded(requestId,
                    "Pas de classification disponible pour cette session. Lancez d'abord une analyse.");
        }

        String controllerRef = session.controllerName();
        String rawSource = LlmServiceSupport.readSourceFile(controllerRef, sourceFileReaderPort);
        String formattedRules = LlmServiceSupport.formatRules(classification);

        SanitizedBundle bundle;
        try {
            bundle = LlmServiceSupport.buildBundle(requestId, rawSource, controllerRef, sanitizationPort);
        } catch (SanitizationRefusedException e) {
            LOG.warn("Revue IA abandonnee pour session {} : {}", sessionId, e.getMessage());
            return ArtifactReviewResult.degraded(requestId,
                    "Sanitisation refusee : " + e.getMessage());
        }

        AiEnrichmentRequest request = new AiEnrichmentRequest(
                requestId,
                bundle,
                TaskType.ARTIFACT_REVIEW,
                PROMPT_TEMPLATE,
                Map.of("classifiedRules", formattedRules));

        AiEnrichmentResult result = aiEnrichmentPort.enrich(request);
        return mapToReview(result);
    }

    private ArtifactReviewResult mapToReview(final AiEnrichmentResult result) {
        if (result.degraded()) {
            return new ArtifactReviewResult(
                    result.requestId(), true, result.degradationReason(),
                    -1, Map.of(), Map.of(), List.of(), result.provider());
        }

        int migrationScore = -1;
        List<String> globalSuggestions = List.of();
        Map<String, String> artifactReviews = new HashMap<>();
        Map<String, String> uncertainReclassifications = new HashMap<>();

        for (Map.Entry<String, String> entry : result.suggestions().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().replace("\r\n", "\n").replace("\r", "\n");
            if ("migrationScore".equals(key)) {
                try {
                    migrationScore = Integer.parseInt(value.trim());
                } catch (NumberFormatException ignored) {
                }
            } else if ("global".equals(key)) {
                globalSuggestions = List.of(value.split("\n"));
            } else if (key.startsWith("uncertain_")) {
                uncertainReclassifications.put(key.substring(10), value);
            } else {
                artifactReviews.put(key, value);
            }
        }

        return new ArtifactReviewResult(
                result.requestId(), false, "",
                migrationScore,
                Map.copyOf(artifactReviews),
                Map.copyOf(uncertainReclassifications),
                globalSuggestions,
                result.provider());
    }
}
