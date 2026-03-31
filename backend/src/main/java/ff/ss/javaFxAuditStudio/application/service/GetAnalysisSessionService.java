package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.GetAnalysisSessionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

import java.util.Objects;
import java.util.Optional;

public final class GetAnalysisSessionService implements GetAnalysisSessionUseCase {

    private final AnalysisSessionPort analysisSessionPort;

    public GetAnalysisSessionService(final AnalysisSessionPort analysisSessionPort) {
        this.analysisSessionPort = Objects.requireNonNull(analysisSessionPort, "analysisSessionPort must not be null");
    }

    @Override
    public Optional<AnalysisSession> handle(final String sessionId) {
        Optional<AnalysisSession> session;

        Objects.requireNonNull(sessionId, "sessionId must not be null");
        session = analysisSessionPort.findById(sessionId);
        return session;
    }
}
