package ff.ss.javaFxAuditStudio.domain.restitution;

import java.util.List;
import java.util.Objects;

/**
 * Rapport complet de restitution pour un controller JavaFX.
 * Contient la synthese, les contradictions detectees, les inconnues
 * et les findings exploitables par le backend, le frontend et le moteur.
 *
 * @param summary         synthese courte de l'analyse
 * @param contradictions  liste des contradictions detectees (peut etre vide)
 * @param unknowns        liste des inconnues non resolues (peut etre vide)
 * @param findings        liste des observations et recommandations (peut etre vide)
 */
public record RestitutionReport(
        RestitutionSummary summary,
        List<String> contradictions,
        List<String> unknowns,
        List<String> findings) {

    public RestitutionReport {
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(contradictions, "contradictions must not be null");
        Objects.requireNonNull(unknowns, "unknowns must not be null");
        Objects.requireNonNull(findings, "findings must not be null");
        contradictions = List.copyOf(contradictions);
        unknowns = List.copyOf(unknowns);
        findings = List.copyOf(findings);
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
