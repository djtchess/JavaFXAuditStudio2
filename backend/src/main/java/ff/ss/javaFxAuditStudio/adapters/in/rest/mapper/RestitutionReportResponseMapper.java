package ff.ss.javaFxAuditStudio.adapters.in.rest.mapper;

import org.springframework.stereotype.Component;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.RestitutionReportResponse;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;

@Component
public class RestitutionReportResponseMapper {

    public RestitutionReportResponse toResponse(final RestitutionReport report) {
        return new RestitutionReportResponse(
                report.summary().controllerRef(),
                report.summary().ruleCount(),
                report.summary().artifactCount(),
                report.summary().confidence().name(),
                report.isActionable(),
                report.findings(),
                report.unknowns());
    }
}
