package ff.ss.javaFxAuditStudio.application.service;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ff.ss.javaFxAuditStudio.application.ports.out.LlmAuditPort;
import ff.ss.javaFxAuditStudio.domain.ai.LlmAuditEntry;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListLlmAuditEntriesServiceTest {

    @Mock
    private LlmAuditPort llmAuditPort;

    @InjectMocks
    private ListLlmAuditEntriesService service;

    @Test
    void handle_should_delegate_to_outbound_port() {
        LlmAuditEntry entry = new LlmAuditEntry(
                "audit-1",
                "session-1",
                Instant.parse("2026-03-23T10:00:00Z"),
                LlmProvider.CLAUDE_CODE,
                TaskType.NAMING,
                "1.0",
                "abc123abc123abc123abc123abc123abc123abc123abc123abc123abc123abcd",
                42,
                false,
                "");
        when(llmAuditPort.findBySessionId("session-1")).thenReturn(List.of(entry));

        List<LlmAuditEntry> result = service.handle("session-1");

        assertThat(result).containsExactly(entry);
        verify(llmAuditPort).findBySessionId("session-1");
    }
}
