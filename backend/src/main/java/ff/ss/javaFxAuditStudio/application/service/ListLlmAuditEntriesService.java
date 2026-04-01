package ff.ss.javaFxAuditStudio.application.service;

import java.util.List;
import java.util.Objects;

import ff.ss.javaFxAuditStudio.application.ports.in.ListLlmAuditEntriesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.LlmAuditPort;
import ff.ss.javaFxAuditStudio.domain.ai.LlmAuditEntry;

/**
 * Service applicatif de consultation des entrees d'audit LLM.
 */
public final class ListLlmAuditEntriesService implements ListLlmAuditEntriesUseCase {

    private final LlmAuditPort llmAuditPort;

    public ListLlmAuditEntriesService(final LlmAuditPort llmAuditPort) {
        this.llmAuditPort = Objects.requireNonNull(llmAuditPort, "llmAuditPort must not be null");
    }

    @Override
    public List<LlmAuditEntry> handle(final String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        return llmAuditPort.findBySessionId(sessionId);
    }
}
