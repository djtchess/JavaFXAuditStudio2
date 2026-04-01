package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.rules.ReclassificationAuditEntry;

import java.util.List;

public interface GetReclassificationHistoryUseCase {

    List<ReclassificationAuditEntry> handle(String analysisId, String ruleId);
}
