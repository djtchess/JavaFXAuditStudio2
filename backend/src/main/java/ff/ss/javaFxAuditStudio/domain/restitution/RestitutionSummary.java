package ff.ss.javaFxAuditStudio.domain.restitution;

import java.util.Objects;

/**
 * Synthese courte d'une restitution pour un controller donne.
 *
 * @param controllerRef    reference du controller analyse
 * @param ruleCount        nombre de regles metier identifiees
 * @param uncertainCount   nombre de regles dont la classification est incertaine
 * @param artifactCount    nombre d'artefacts generes
 * @param bridgeCount      nombre de bridges transitionnels generes
 * @param confidence       niveau de confiance global de l'analyse
 * @param hasContradictions indique si des contradictions ont ete detectees
 */
public record RestitutionSummary(
        String controllerRef,
        int ruleCount,
        int uncertainCount,
        int artifactCount,
        int bridgeCount,
        ConfidenceLevel confidence,
        boolean hasContradictions) {

    public RestitutionSummary {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(confidence, "confidence must not be null");
    }
}
