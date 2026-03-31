package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

import java.util.Optional;

public interface GetAnalysisSessionUseCase {

    Optional<AnalysisSession> handle(String sessionId);
}
