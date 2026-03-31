package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.SubmitAnalysisUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionStatusHistoryPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatusTransition;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class SubmitAnalysisService implements SubmitAnalysisUseCase {

    private final AnalysisSessionPort analysisSessionPort;
    private final AnalysisSessionStatusHistoryPort statusHistoryPort;

    public SubmitAnalysisService(
            final AnalysisSessionPort analysisSessionPort,
            final AnalysisSessionStatusHistoryPort statusHistoryPort) {
        this.analysisSessionPort = Objects.requireNonNull(analysisSessionPort, "analysisSessionPort must not be null");
        this.statusHistoryPort = (statusHistoryPort != null) ? statusHistoryPort : AnalysisSessionStatusHistoryPort.noop();
    }

    @Override
    public AnalysisSession handle(final List<String> sourceFilePaths, final String sessionName) {
        List<String> sanitizedPaths;
        Instant createdAt;
        String sessionId;
        String controllerRef;
        String sourceRef;
        AnalysisSession session;

        sanitizedPaths = validatePaths(sourceFilePaths);
        createdAt = Instant.now();
        sessionId = UUID.randomUUID().toString();
        controllerRef = selectControllerRef(sanitizedPaths);
        sourceRef = selectSourceRef(sanitizedPaths);
        session = new AnalysisSession(
                sessionId,
                sanitizeSessionName(sessionName, controllerRef),
                controllerRef,
                sourceRef,
                AnalysisStatus.CREATED,
                createdAt);
        session = analysisSessionPort.save(session);
        statusHistoryPort.save(new AnalysisStatusTransition(session.sessionId(), session.status(), createdAt));
        return session;
    }

    private static List<String> validatePaths(final List<String> sourceFilePaths) {
        List<String> sanitizedPaths;

        if (sourceFilePaths == null) {
            throw new IllegalArgumentException("sourceFilePaths must not be null");
        }
        sanitizedPaths = sourceFilePaths.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(path -> !path.isBlank())
                .toList();
        if (sanitizedPaths.isEmpty()) {
            throw new IllegalArgumentException("sourceFilePaths must not be empty");
        }
        return sanitizedPaths;
    }

    private static String sanitizeSessionName(final String sessionName, final String controllerRef) {
        String sanitized;

        sanitized = (sessionName != null) ? sessionName.trim() : "";
        if (sanitized.isBlank()) {
            sanitized = controllerRef;
        }
        return sanitized;
    }

    private static String selectControllerRef(final List<String> sourceFilePaths) {
        return sourceFilePaths.stream()
                .filter(path -> path.endsWith(".java"))
                .findFirst()
                .orElse(sourceFilePaths.get(0));
    }

    private static String selectSourceRef(final List<String> sourceFilePaths) {
        return sourceFilePaths.stream()
                .filter(path -> path.endsWith(".fxml"))
                .findFirst()
                .orElse(null);
    }
}
