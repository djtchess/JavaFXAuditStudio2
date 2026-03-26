package ff.ss.javaFxAuditStudio.application.service;

import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.in.PreviewSanitizedSourceUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedSourcePreviewResult;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationReport;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

/**
 * Service applicatif de previsualisation du code sanitise (JAS-031 / AI-2).
 *
 * <p>Lit le fichier source via la session, applique la sanitisation si le port
 * est disponible, sinon retourne le code brut dans un {@link SanitizedBundle}.
 * Aucun appel LLM n'est effectue.
 */
public class PreviewSanitizedSourceService implements PreviewSanitizedSourceUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(PreviewSanitizedSourceService.class);

    private final AnalysisSessionPort sessionPort;
    private final SanitizationPort sanitizationPort; // nullable
    private final SourceFileReaderPort sourceFileReaderPort; // nullable

    public PreviewSanitizedSourceService(
            final AnalysisSessionPort sessionPort,
            final SanitizationPort sanitizationPort) {
        this(sessionPort, sanitizationPort, null);
    }

    public PreviewSanitizedSourceService(
            final AnalysisSessionPort sessionPort,
            final SanitizationPort sanitizationPort,
            final SourceFileReaderPort sourceFileReaderPort) {
        this.sessionPort = Objects.requireNonNull(sessionPort, "sessionPort must not be null");
        this.sanitizationPort = sanitizationPort;
        this.sourceFileReaderPort = sourceFileReaderPort;
    }

    @Override
    public SanitizedSourcePreviewResult preview(final String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");

        AnalysisSession session = sessionPort.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));

        String controllerRef = session.controllerName();
        String rawSource = LlmServiceSupport.readSourceFile(controllerRef, sourceFileReaderPort);
        String bundleId = UUID.randomUUID().toString();

        if (sanitizationPort != null) {
            LOG.debug("Preview sanitise — session={} controllerRef={}", sessionId, controllerRef);
            return sanitizeOrFallback(bundleId, rawSource, controllerRef, sessionId);
        }

        LOG.debug("Preview brut (sanitisation desactivee) — session={}", sessionId);
        SanitizedBundle bundle = new SanitizedBundle(
                bundleId,
                controllerRef,
                rawSource,
                LlmServiceSupport.estimateTokens(rawSource),
                LlmServiceSupport.SANITIZATION_VERSION,
                null);
        return new SanitizedSourcePreviewResult(bundle, false);
    }

    /**
     * Mode dry-run : collecte les transformations sans lever de
     * {@link SanitizationRefusedException} ni appeler le LLM.
     *
     * <p>Si {@link SanitizationPort} est absent, retourne un rapport approuve vide.
     */
    @Override
    public SanitizationReport previewDryRun(final String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");

        AnalysisSession session = sessionPort.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));

        String controllerRef = session.controllerName();
        String rawSource = LlmServiceSupport.readSourceFile(controllerRef, sourceFileReaderPort);
        String bundleId = UUID.randomUUID().toString();

        if (sanitizationPort == null) {
            LOG.debug("DryRun : sanitisation desactivee — session={}", sessionId);
            return SanitizationReport.approved(bundleId, LlmServiceSupport.SANITIZATION_VERSION,
                    java.util.List.of());
        }

        LOG.debug("DryRun preview — session={} controllerRef={}", sessionId, controllerRef);
        return sanitizationPort.previewTransformations(bundleId, rawSource, controllerRef);
    }

    /**
     * Tente la sanitisation et retourne un resultat sanitise.
     * En cas de {@link SanitizationRefusedException}, retourne un bundle brut avec
     * {@code sanitized=false} sans propagation.
     * Un {@link IllegalArgumentException} (rawSource blank) est toujours propage.
     */
    private SanitizedSourcePreviewResult sanitizeOrFallback(
            final String bundleId,
            final String rawSource,
            final String controllerRef,
            final String sessionId) {
        try {
            SanitizedBundle bundle = sanitizationPort.sanitize(bundleId, rawSource, controllerRef);
            return new SanitizedSourcePreviewResult(bundle, true);
        } catch (SanitizationRefusedException ex) {
            LOG.warn("Sanitisation refusee pour session={} — retour bundle brut : {}",
                    sessionId, ex.getMessage());
            SanitizedBundle fallback = new SanitizedBundle(
                    bundleId,
                    controllerRef,
                    rawSource,
                    LlmServiceSupport.estimateTokens(rawSource),
                    LlmServiceSupport.SANITIZATION_VERSION,
                    null);
            return new SanitizedSourcePreviewResult(fallback, false);
        }
    }
}
