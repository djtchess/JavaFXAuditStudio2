package ff.ss.javaFxAuditStudio.adapters.out.restitution;

import org.springframework.stereotype.Component;

import ff.ss.javaFxAuditStudio.application.ports.out.RestitutionFormatterPort;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionSummary;

import java.util.List;

/**
 * Adaptateur sortant produisant un rapport Markdown lisible par developpeurs et PO.
 * Utilise un StringBuilder pour eviter toute dependance a un moteur de template.
 */
@Component
public class MarkdownRestitutionFormatterAdapter implements RestitutionFormatterPort {

    private static final String LINE_BREAK = "\n";
    private static final String SECTION_BREAK = "\n\n";

    @Override
    public String formatAsMarkdown(final RestitutionReport report) {
        StringBuilder sb;
        RestitutionSummary summary;

        sb = new StringBuilder();
        summary = report.summary();

        appendHeader(sb, summary);
        appendExecutiveSummary(sb, summary);
        appendSummaryTable(sb, summary);
        appendUnknowns(sb, report);
        appendFindings(sb, report);
        appendArtifactsSection(sb, report);

        return sb.toString();
    }

    private void appendHeader(final StringBuilder sb, final RestitutionSummary summary) {
        sb.append("# Restitution").append(LINE_BREAK);
        sb.append("Controller : `").append(summary.controllerRef()).append("`");
        sb.append(SECTION_BREAK);
        sb.append("## Synthese").append(LINE_BREAK);
    }

    private void appendExecutiveSummary(final StringBuilder sb, final RestitutionSummary summary) {
        sb.append("## Résumé exécutif").append(LINE_BREAK);
        sb.append("Analyse du controller `").append(summary.controllerRef()).append("` : ");
        sb.append(summary.ruleCount()).append(" règles identifiées, ");
        sb.append("dont ").append(summary.uncertainCount()).append(" incertaines.").append(LINE_BREAK);
        sb.append("Niveau de confiance : **").append(summary.confidence()).append("**.");
        if (summary.hasContradictions()) {
            sb.append(LINE_BREAK);
            sb.append("⚠️ Des contradictions ont été détectées.");
        }
        sb.append(SECTION_BREAK);
    }

    private void appendSummaryTable(final StringBuilder sb, final RestitutionSummary summary) {
        sb.append("| Metrique | Valeur |").append(LINE_BREAK);
        sb.append("|---|---|").append(LINE_BREAK);
        sb.append("| Regles metier | ").append(summary.ruleCount()).append(" |").append(LINE_BREAK);
        sb.append("| Artefacts generes | ").append(summary.artifactCount()).append(" |").append(LINE_BREAK);
        sb.append("| Niveau de confiance | ").append(summary.confidence()).append(" |").append(LINE_BREAK);
        sb.append("| Contradictions | ").append(summary.hasContradictions() ? "Oui" : "Non").append(" |");
        sb.append(SECTION_BREAK);
    }

    private void appendUnknowns(final StringBuilder sb, final RestitutionReport report) {
        sb.append("## Inconnues").append(LINE_BREAK);
        if (report.unknowns().isEmpty()) {
            sb.append("_Aucune inconnue detectee._");
        } else {
            report.unknowns().forEach(u -> sb.append("- ").append(u).append(LINE_BREAK));
        }
        sb.append(SECTION_BREAK);
    }

    private void appendFindings(final StringBuilder sb, final RestitutionReport report) {
        sb.append("## Findings").append(LINE_BREAK);
        if (report.findings().isEmpty()) {
            sb.append("_Aucun finding enregistre._");
        } else {
            report.findings().forEach(f -> sb.append("- ").append(f).append(LINE_BREAK));
        }
        sb.append(LINE_BREAK);
    }

    private void appendArtifactsSection(final StringBuilder sb, final RestitutionReport report) {
        sb.append("## Artefacts").append(LINE_BREAK);
        List<String> artifactFindings = report.findings().stream()
                .filter(f -> f.toLowerCase().contains("artefact") || f.toLowerCase().contains("artifact"))
                .toList();
        if (artifactFindings.isEmpty()) {
            sb.append("_Aucun artefact généré._");
        } else {
            artifactFindings.forEach(f -> sb.append("- ").append(f).append(LINE_BREAK));
        }
        sb.append(LINE_BREAK);
    }
}
