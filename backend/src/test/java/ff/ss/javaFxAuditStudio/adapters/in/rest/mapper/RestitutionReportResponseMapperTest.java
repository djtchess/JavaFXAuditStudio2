package ff.ss.javaFxAuditStudio.adapters.in.rest.mapper;

import ff.ss.javaFxAuditStudio.domain.restitution.ConfidenceLevel;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RestitutionReportResponseMapperTest {

    @Test
    void toResponse_shouldExposeMarkdownAndCoreMetrics() {
        RestitutionSummary summary = new RestitutionSummary(
                "SampleController",
                5,
                2,
                3,
                1,
                ConfidenceLevel.HIGH,
                false);
        RestitutionReport report = new RestitutionReport(
                summary,
                List.of("Contradiction 1"),
                List.of("Unknown 1"),
                List.of("Finding 1"),
                "# Restitution\n\n## Synthese");

        RestitutionReportResponseMapper mapper = new RestitutionReportResponseMapper();

        var response = mapper.toResponse(report);

        assertThat(response.controllerRef()).isEqualTo("SampleController");
        assertThat(response.ruleCount()).isEqualTo(5);
        assertThat(response.artifactCount()).isEqualTo(3);
        assertThat(response.confidence()).isEqualTo("HIGH");
        assertThat(response.isActionable()).isTrue();
        assertThat(response.findings()).containsExactly("Finding 1");
        assertThat(response.unknowns()).containsExactly("Unknown 1");
        assertThat(response.markdown()).contains("## Synthese");
    }
}
