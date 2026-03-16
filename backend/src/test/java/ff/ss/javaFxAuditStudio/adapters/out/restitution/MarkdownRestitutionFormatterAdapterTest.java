package ff.ss.javaFxAuditStudio.adapters.out.restitution;

import ff.ss.javaFxAuditStudio.domain.restitution.ConfidenceLevel;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownRestitutionFormatterAdapterTest {

    private MarkdownRestitutionFormatterAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MarkdownRestitutionFormatterAdapter();
    }

    private RestitutionReport buildTestReport() {
        RestitutionSummary summary = new RestitutionSummary(
                "SampleController", 5, 2, 3, 1,
                ConfidenceLevel.MEDIUM, false);
        return new RestitutionReport(summary, List.of(), List.of("Inconnue 1"),
                List.of("artefact: ViewModel généré", "Finding métier 1"));
    }

    @Test
    void shouldContainExecutiveSummarySection() {
        RestitutionReport report = buildTestReport();
        String result = adapter.formatAsMarkdown(report);

        assertThat(result).contains("Résumé exécutif");
    }

    @Test
    void shouldContainArtifactsSection() {
        RestitutionReport report = buildTestReport();
        String result = adapter.formatAsMarkdown(report);

        assertThat(result).contains("Artefacts");
    }

    @Test
    void shouldContainControllerRefInHeader() {
        RestitutionReport report = buildTestReport();
        String result = adapter.formatAsMarkdown(report);

        assertThat(result).contains("SampleController");
    }

    @Test
    void shouldContainAllFourSections() {
        RestitutionReport report = buildTestReport();
        String result = adapter.formatAsMarkdown(report);

        assertThat(result).contains("Synthese");
        assertThat(result).contains("Inconnues");
        assertThat(result).contains("Findings");
        assertThat(result).contains("Artefacts");
    }
}
