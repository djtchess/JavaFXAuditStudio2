package ff.ss.javaFxAuditStudio.application.service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.adapters.out.ai.PayloadHasher;
import ff.ss.javaFxAuditStudio.application.ports.in.EnrichAnalysisUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.LlmAuditPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.configuration.LlmAuditProperties;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.LlmAuditEntry;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

/**
 * Service applicatif d'enrichissement IA (JAS-017 / JAS-018 / JAS-029 / IAP-2).
 *
 * <p>Assemble via {@code AiEnrichmentOrchestraConfiguration} — pas de {@code @Service}.
 */
public class EnrichAnalysisService implements EnrichAnalysisUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(EnrichAnalysisService.class);
    private static final String DEFAULT_PROMPT_TEMPLATE = "enrichment-default";

    private final AnalysisSessionPort sessionPort;
    private final AiEnrichmentPort enrichmentPort;
    private final SanitizationPort sanitizationPort;
    private final LlmAuditPort auditPort;
    private final PayloadHasher hasher;
    private final LlmAuditProperties auditProperties;
    private final SourceFileReaderPort sourceFileReaderPort;

    /**
     * Constructeur de compatibilité sans audit (JAS-017 / JAS-018).
     */
    public EnrichAnalysisService(
            final AnalysisSessionPort sessionPort,
            final AiEnrichmentPort enrichmentPort,
            final SanitizationPort sanitizationPort) {
        this(sessionPort, enrichmentPort, sanitizationPort, null, null, null, null);
    }

    /**
     * Constructeur complet avec sanitisation, audit et lecture de source (IAP-5).
     */
    public EnrichAnalysisService(
            final AnalysisSessionPort sessionPort,
            final AiEnrichmentPort enrichmentPort,
            final SanitizationPort sanitizationPort,
            final LlmAuditPort auditPort,
            final PayloadHasher hasher,
            final LlmAuditProperties auditProperties,
            final SourceFileReaderPort sourceFileReaderPort) {
        this.sessionPort = Objects.requireNonNull(sessionPort, "sessionPort must not be null");
        this.enrichmentPort = Objects.requireNonNull(enrichmentPort, "enrichmentPort must not be null");
        this.sanitizationPort = sanitizationPort;
        this.auditPort = auditPort;
        this.hasher = hasher;
        this.auditProperties = auditProperties;
        this.sourceFileReaderPort = sourceFileReaderPort;
    }

    @Override
    public AiEnrichmentResult enrich(final String sessionId, final TaskType taskType) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(taskType, "taskType must not be null");

        AnalysisSession session = sessionPort.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Session introuvable : " + sessionId));

        String requestId = UUID.randomUUID().toString();
        String controllerRef = session.controllerName();
        String rawSource = LlmServiceSupport.readSourceFile(controllerRef, sourceFileReaderPort);
        String bundleId = UUID.randomUUID().toString();

        SanitizedBundle bundle;
        try {
            bundle = LlmServiceSupport.buildBundle(bundleId, rawSource, controllerRef, sanitizationPort);
        } catch (SanitizationRefusedException e) {
            LOG.warn("Enrichissement abandonne pour session {} : {}", sessionId, e.getMessage());
            return AiEnrichmentResult.degraded(requestId,
                    "Sanitisation refusee : marqueur sensible detecte");
        }

        AiEnrichmentRequest request = new AiEnrichmentRequest(
                requestId,
                bundle,
                taskType,
                DEFAULT_PROMPT_TEMPLATE);

        AiEnrichmentResult result = enrichmentPort.enrich(request);

        saveAudit(sessionId, taskType, bundle, result);

        return result;
    }

    private void saveAudit(
            final String sessionId,
            final TaskType taskType,
            final SanitizedBundle bundle,
            final AiEnrichmentResult result) {
        if (auditPort == null || hasher == null || auditProperties == null || !auditProperties.enabled()) {
            return;
        }
        try {
            LlmAuditEntry entry = new LlmAuditEntry(
                    UUID.randomUUID().toString(),
                    sessionId,
                    Instant.now(),
                    result.provider(),
                    taskType,
                    bundle.sanitizationVersion(),
                    hasher.hash(bundle.sanitizedSource()),
                    bundle.estimatedTokens(),
                    result.degraded(),
                    result.degradationReason());
            auditPort.save(entry);
        } catch (Exception e) {
            LOG.warn("Echec de la persistance de l'audit LLM pour session {} : {}", sessionId, e.getMessage());
        }
    }
}
