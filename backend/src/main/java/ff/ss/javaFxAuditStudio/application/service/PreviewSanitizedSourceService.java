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
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

/**
 * Service applicatif de previsualisation du code sanitise (JAS-031).
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
            SanitizedBundle bundle = sanitizationPort.sanitize(bundleId, rawSource, controllerRef);
            return new SanitizedSourcePreviewResult(bundle, true);
        }

        LOG.debug("Preview brut (sanitisation desactivee) — session={}", sessionId);
        SanitizedBundle bundle = new SanitizedBundle(
                bundleId,
                controllerRef,
                rawSource,
                LlmServiceSupport.estimateTokens(rawSource),
                LlmServiceSupport.SANITIZATION_VERSION);
        return new SanitizedSourcePreviewResult(bundle, false);
    }
}
