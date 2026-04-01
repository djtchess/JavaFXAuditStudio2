package ff.ss.javaFxAuditStudio.application.ports.in;

import java.util.List;

import ff.ss.javaFxAuditStudio.domain.ai.LlmAuditEntry;

/**
 * Consultation des entrees d'audit LLM pour une session.
 */
public interface ListLlmAuditEntriesUseCase {

    List<LlmAuditEntry> handle(String sessionId);
}
