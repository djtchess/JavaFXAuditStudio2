package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.domain.rules.ReclassificationAuditEntry;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetReclassificationHistoryServiceTest {

    @Mock
    private ReclassificationAuditPort reclassificationAuditPort;

    @Test
    void handle_returnsHistoryFromPort() {
        GetReclassificationHistoryService service = new GetReclassificationHistoryService(reclassificationAuditPort);
        ReclassificationAuditEntry entry = new ReclassificationAuditEntry(
                "audit-1",
                "analysis-1",
                "rule-1",
                ResponsibilityClass.UI,
                ResponsibilityClass.APPLICATION,
                "raison",
                Instant.parse("2026-01-15T10:00:00Z"));

        when(reclassificationAuditPort.findByAnalysisIdAndRuleId("analysis-1", "rule-1"))
                .thenReturn(List.of(entry));

        List<ReclassificationAuditEntry> result = service.handle("analysis-1", "rule-1");

        assertThat(result).containsExactly(entry);
        verify(reclassificationAuditPort).findByAnalysisIdAndRuleId("analysis-1", "rule-1");
    }

    @Test
    void handle_throwsWhenAnalysisIdIsNull() {
        GetReclassificationHistoryService service = new GetReclassificationHistoryService(reclassificationAuditPort);

        assertThatThrownBy(() -> service.handle(null, "rule-1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("analysisId must not be null");
    }

    @Test
    void handle_throwsWhenRuleIdIsNull() {
        GetReclassificationHistoryService service = new GetReclassificationHistoryService(reclassificationAuditPort);

        assertThatThrownBy(() -> service.handle("analysis-1", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ruleId must not be null");
    }
}
