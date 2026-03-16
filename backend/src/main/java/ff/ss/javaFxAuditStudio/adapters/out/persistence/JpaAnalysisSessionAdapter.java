package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaAnalysisSessionAdapter implements AnalysisSessionPort {

    private final AnalysisSessionRepository repository;

    public JpaAnalysisSessionAdapter(AnalysisSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public AnalysisSession save(AnalysisSession session) {
        AnalysisSessionEntity saved = repository.save(toEntity(session));
        return toDomain(saved);
    }

    @Override
    public Optional<AnalysisSession> findById(String sessionId) {
        return repository.findById(sessionId).map(this::toDomain);
    }

    @Override
    public List<AnalysisSession> findAll() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    private AnalysisSessionEntity toEntity(AnalysisSession session) {
        return new AnalysisSessionEntity(
                session.sessionId(),
                session.controllerName(),
                session.sourceSnippetRef(),
                session.status().name(),
                session.createdAt());
    }

    private AnalysisSession toDomain(AnalysisSessionEntity entity) {
        return new AnalysisSession(
                entity.getSessionId(),
                entity.getControllerName(),
                entity.getSourceSnippetRef(),
                AnalysisStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt());
    }
}
