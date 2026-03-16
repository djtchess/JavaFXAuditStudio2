package ff.ss.javaFxAuditStudio.application.ports.out;

import java.util.List;
import java.util.Optional;

import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

public interface AnalysisSessionPort {

    AnalysisSession save(AnalysisSession session);

    Optional<AnalysisSession> findById(String sessionId);

    List<AnalysisSession> findAll();
}
