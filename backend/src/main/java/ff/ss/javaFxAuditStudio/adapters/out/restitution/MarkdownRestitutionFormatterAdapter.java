package ff.ss.javaFxAuditStudio.adapters.out.restitution;

import org.springframework.stereotype.Component;

import ff.ss.javaFxAuditStudio.application.ports.out.RestitutionFormatterPort;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionSummary;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Adaptateur sortant produisant un rapport Markdown lisible par developpeurs et PO.
 * Structure en sections : Synthese, Metriques, Repartition, Findings, Inconnues, Contradictions.
 */
@Component
public class MarkdownRestitutionFormatterAdapter implements RestitutionFormatterPort {

    private static final String LINE_BREAK = "\n";
    private static final String SECTION_BREAK = "\n\n";
    private static final int BAR_MAX_WIDTH = 30;

    @Override
    public String formatAsMarkdown(final RestitutionReport report) {
        StringBuilder sb = new StringBuilder();
        RestitutionSummary summary = report.summary();
        appendHeader(sb, summary);
        appendSynthesis(sb, summary);
        appendMetrics(sb, summary);
        appendDistribution(sb, summary);
        appendLots(sb, report);
        appendArtifacts(sb, report);
        appendFindings(sb, report);
        appendUnknowns(sb, report);
        appendContradictions(sb, report);
        return sb.toString();
    }

    private void appendHeader(final StringBuilder sb, final RestitutionSummary summary) {
        sb.append("# Restitution").append(LINE_BREAK);
        sb.append("Controller : `").append(summary.controllerRef()).append("`");
        sb.append(SECTION_BREAK);
    }

    private void appendSynthesis(final StringBuilder sb, final RestitutionSummary summary) {
        sb.append("## Synthese").append(LINE_BREAK);
        sb.append("Analyse du controller `").append(summary.controllerRef()).append("` : ");
        sb.append(summary.ruleCount()).append(" regles identifiees, ");
        sb.append("dont ").append(summary.uncertainCount()).append(" incertaines.");
        sb.append(LINE_BREAK);
        sb.append("Niveau de confiance : **").append(summary.confidence()).append("**.");
        if (summary.hasContradictions()) {
            sb.append(LINE_BREAK);
            sb.append("ATTENTION : Des contradictions ont ete detectees.");
        }
        sb.append(SECTION_BREAK);
    }

    private void appendMetrics(final StringBuilder sb, final RestitutionSummary summary) {
        sb.append("## Metriques").append(LINE_BREAK);
        sb.append("| Metrique | Valeur |").append(LINE_BREAK);
        sb.append("|---|---|").append(LINE_BREAK);
        sb.append("| Regles metier | ").append(summary.ruleCount()).append(" |");
        sb.append(LINE_BREAK);
        sb.append("| Regles incertaines | ").append(summary.uncertainCount()).append(" |");
        sb.append(LINE_BREAK);
        sb.append("| Artefacts generes | ").append(summary.artifactCount()).append(" |");
        sb.append(LINE_BREAK);
        sb.append("| Bridges transitionnels | ").append(summary.bridgeCount()).append(" |");
        sb.append(LINE_BREAK);
        sb.append("| Niveau de confiance | ").append(summary.confidence()).append(" |");
        sb.append(LINE_BREAK);
        sb.append("| Contradictions | ");
        sb.append(summary.hasContradictions() ? "Oui" : "Non").append(" |");
        sb.append(SECTION_BREAK);
    }

    private void appendDistribution(final StringBuilder sb, final RestitutionSummary summary) {
        sb.append("## Repartition").append(LINE_BREAK);
        int total = summary.ruleCount();
        if (total == 0) {
            sb.append("_Aucune regle a repartir._");
            sb.append(SECTION_BREAK);
            return;
        }
        int certain = summary.ruleCount() - summary.uncertainCount();
        int uncertain = summary.uncertainCount();
        Map<String, Integer> categories = new LinkedHashMap<>();
        categories.put("Classifiees ", certain);
        categories.put("Incertaines ", uncertain);
        for (Map.Entry<String, Integer> entry : categories.entrySet()) {
            int barLen = computeBarLength(entry.getValue(), total);
            String bar = buildBar(barLen);
            sb.append(entry.getKey()).append(bar);
            sb.append(" ").append(entry.getValue()).append("/").append(total);
            sb.append(LINE_BREAK);
        }
        sb.append(SECTION_BREAK);
    }

    private int computeBarLength(final int value, final int total) {
        if (total == 0) {
            return 0;
        }
        return (int) Math.round((double) value / total * BAR_MAX_WIDTH);
    }

    private String buildBar(final int length) {
        if (length <= 0) {
            return "[]";
        }
        return "[" + "#".repeat(length) + "]";
    }

    private void appendFindings(final StringBuilder sb, final RestitutionReport report) {
        sb.append("## Findings").append(LINE_BREAK);
        if (report.findings().isEmpty()) {
            sb.append("_Aucun finding enregistre._");
        } else {
            appendBulletList(sb, report.findings());
        }
        sb.append(SECTION_BREAK);
    }

    private void appendLots(final StringBuilder sb, final RestitutionReport report) {
        sb.append("## Lots").append(LINE_BREAK);
        if (report.lotSummaries().isEmpty()) {
            sb.append("_Aucun lot de migration disponible._");
        } else {
            appendBulletList(sb, report.lotSummaries());
        }
        sb.append(SECTION_BREAK);
    }

    private void appendArtifacts(final StringBuilder sb, final RestitutionReport report) {
        sb.append("## Artefacts").append(LINE_BREAK);
        if (report.artifactSummaries().isEmpty()) {
            sb.append("_Aucun artefact genere._");
        } else {
            appendBulletList(sb, report.artifactSummaries());
        }
        sb.append(SECTION_BREAK);
    }

    private void appendUnknowns(final StringBuilder sb, final RestitutionReport report) {
        sb.append("## Inconnues").append(LINE_BREAK);
        if (report.unknowns().isEmpty()) {
            sb.append("_Aucune inconnue detectee._");
        } else {
            appendBulletList(sb, report.unknowns());
        }
        sb.append(SECTION_BREAK);
    }

    private void appendContradictions(final StringBuilder sb, final RestitutionReport report) {
        sb.append("## Contradictions").append(LINE_BREAK);
        if (report.contradictions().isEmpty()) {
            sb.append("_Aucune contradiction detectee._");
        } else {
            appendBulletList(sb, report.contradictions());
        }
        sb.append(LINE_BREAK);
    }

    private void appendBulletList(final StringBuilder sb, final List<String> items) {
        for (String item : items) {
            sb.append("- ").append(item).append(LINE_BREAK);
        }
    }
}
