package ff.ss.javaFxAuditStudio.application.service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.in.GenerateSpringBootClassesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentRequest;
import ff.ss.javaFxAuditStudio.domain.ai.AiEnrichmentResult;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;
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
    private final AiEnrichmentPort aiEnrichmentPort;
    private final SanitizationPort sanitizationPort;
    private final SourceFileReaderPort sourceFileReaderPort;

    public AiSpringBootGenerationService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort) {
        this(sessionPort, classificationPort, aiEnrichmentPort, sanitizationPort, null);
    }

    public AiSpringBootGenerationService(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SourceFileReaderPort sourceFileReaderPort) {
        this.sessionPort = Objects.requireNonNull(sessionPort, "sessionPort must not be null");
        this.classificationPort = Objects.requireNonNull(classificationPort, "classificationPort must not be null");
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
                Map.of("classifiedRules", formattedRules));

        AiEnrichmentResult llmResult = aiEnrichmentPort.enrich(request);

        return mapToGenerationResult(llmResult);
    }

    private AiCodeGenerationResult mapToGenerationResult(final AiEnrichmentResult llmResult) {
        if (llmResult.degraded()) {
            return new AiCodeGenerationResult(
                    llmResult.requestId(),
                    true,
                    llmResult.degradationReason(),
                    Map.of(),
                    0,
                    llmResult.provider());
        }

        Map<String, String> cleanedClasses = llmResult.suggestions().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().replace("\r\n", "\n").replace("\r", "\n")));

        return new AiCodeGenerationResult(
                llmResult.requestId(),
                false,
                "",
                cleanedClasses,
                llmResult.tokensUsed(),
                llmResult.provider());
    }
}
