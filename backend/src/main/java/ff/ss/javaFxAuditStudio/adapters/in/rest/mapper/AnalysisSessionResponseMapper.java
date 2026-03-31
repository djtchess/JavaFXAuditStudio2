package ff.ss.javaFxAuditStudio.adapters.in.rest.mapper;

import org.springframework.stereotype.Component;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AnalysisSessionResponse;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

@Component
public class AnalysisSessionResponseMapper {

    public AnalysisSessionResponse toResponse(final AnalysisSession session) {
        return new AnalysisSessionResponse(
                session.sessionId(),
                session.status().name(),
                session.sessionName(),
                session.controllerName(),
                session.sourceSnippetRef(),
                session.createdAt());
    }
}
