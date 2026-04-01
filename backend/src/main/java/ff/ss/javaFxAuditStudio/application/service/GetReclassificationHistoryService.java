package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.GetReclassificationHistoryUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.domain.rules.ReclassificationAuditEntry;

import java.util.List;
import java.util.Objects;

public final class GetReclassificationHistoryService implements GetReclassificationHistoryUseCase {

    private final ReclassificationAuditPort reclassificationAuditPort;

    public GetReclassificationHistoryService(final ReclassificationAuditPort reclassificationAuditPort) {
        this.reclassificationAuditPort = Objects.requireNonNull(
                reclassificationAuditPort, "reclassificationAuditPort must not be null");
    }

    @Override
    public List<ReclassificationAuditEntry> handle(final String analysisId, final String ruleId) {
        Objects.requireNonNull(analysisId, "analysisId must not be null");
        Objects.requireNonNull(ruleId, "ruleId must not be null");

        return reclassificationAuditPort.findByAnalysisIdAndRuleId(analysisId, ruleId);
    }
}
