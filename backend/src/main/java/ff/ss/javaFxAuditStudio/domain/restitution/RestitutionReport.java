package ff.ss.javaFxAuditStudio.domain.restitution;

import java.util.List;
import java.util.Objects;

/**
 * Rapport complet de restitution pour un controller JavaFX.
 * Contient la synthese, les contradictions detectees, les inconnues
 * les findings exploitables par le backend, le frontend et le moteur,
 * ainsi que le markdown genere pour la restitution lisible.
 *
 * @param summary         synthese courte de l'analyse
 * @param contradictions  liste des contradictions detectees (peut etre vide)
 * @param unknowns        liste des inconnues non resolues (peut etre vide)
 * @param findings        liste des observations et recommandations (peut etre vide)
 * @param markdown        version markdown de la restitution
 */
public record RestitutionReport(
        RestitutionSummary summary,
        List<String> contradictions,
        List<String> unknowns,
        List<String> findings,
        String markdown) {

    public RestitutionReport {
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(contradictions, "contradictions must not be null");
        Objects.requireNonNull(unknowns, "unknowns must not be null");
        Objects.requireNonNull(findings, "findings must not be null");
        contradictions = List.copyOf(contradictions);
        unknowns = List.copyOf(unknowns);
        findings = List.copyOf(findings);
        markdown = markdown == null ? "" : markdown;
    }

    public RestitutionReport(
            final RestitutionSummary summary,
            final List<String> contradictions,
            final List<String> unknowns,
            final List<String> findings) {
        this(summary, contradictions, unknowns, findings, "");
    }

    /**
     * Indique si le rapport est actionnable par les equipes.
     * Un rapport est actionnable si le niveau de confiance est suffisant
     * et qu'il ne contient pas de contradictions bloquantes.
     *
     * @return vrai si le rapport peut etre exploite sans arbitrage prealable
     */
    public boolean isActionable() {
        return summary.confidence() != ConfidenceLevel.INSUFFICIENT && !summary.hasContradictions();
    }
}
