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
        return new RestitutionReport(summary,
                List.of(),
                List.of("Inconnue 1"),
                List.of("Finding metier 1", "artefact: ViewModel genere"));
    }

    @Test
    void shouldContainControllerRefInHeader() {
        RestitutionReport report = buildTestReport();
        String result = adapter.formatAsMarkdown(report);

        assertThat(result).contains("SampleController");
    }

    @Test
    void shouldContainAllSixSections() {
        RestitutionReport report = buildTestReport();
        String result = adapter.formatAsMarkdown(report);

        assertThat(result).contains("## Synthese");
        assertThat(result).contains("## Metriques");
        assertThat(result).contains("## Repartition");
        assertThat(result).contains("## Findings");
        assertThat(result).contains("## Inconnues");
        assertThat(result).contains("## Contradictions");
    }

    @Test
    void shouldContainMetricsTable() {
        RestitutionReport report = buildTestReport();
        String result = adapter.formatAsMarkdown(report);

        assertThat(result).contains("| Regles metier | 5 |");
        assertThat(result).contains("| Regles incertaines | 2 |");
        assertThat(result).contains("| Artefacts generes | 3 |");
        assertThat(result).contains("| Bridges transitionnels | 1 |");
    }

    @Test
    void shouldContainDistributionBars() {
        RestitutionReport report = buildTestReport();
        String result = adapter.formatAsMarkdown(report);

        assertThat(result).contains("Classifiees ");
        assertThat(result).contains("Incertaines ");
        assertThat(result).contains("#");
    }

    @Test
    void shouldContainFindings() {
        RestitutionReport report = buildTestReport();
        String result = adapter.formatAsMarkdown(report);

        assertThat(result).contains("- Finding metier 1");
    }

    @Test
    void shouldContainUnknowns() {
        RestitutionReport report = buildTestReport();
        String result = adapter.formatAsMarkdown(report);

        assertThat(result).contains("- Inconnue 1");
    }

    @Test
    void shouldShowNoContradictionMessage() {
        RestitutionReport report = buildTestReport();
        String result = adapter.formatAsMarkdown(report);

        assertThat(result).contains("_Aucune contradiction detectee._");
    }

    @Test
    void shouldShowContradictionsWhenPresent() {
        RestitutionSummary summary = new RestitutionSummary(
                "TestController", 3, 1, 2, 0,
                ConfidenceLevel.LOW, true);
        RestitutionReport report = new RestitutionReport(summary,
                List.of("Contradiction 1"),
                List.of(), List.of());
        String result = adapter.formatAsMarkdown(report);

        assertThat(result).contains("- Contradiction 1");
        assertThat(result).contains("ATTENTION");
    }
}
