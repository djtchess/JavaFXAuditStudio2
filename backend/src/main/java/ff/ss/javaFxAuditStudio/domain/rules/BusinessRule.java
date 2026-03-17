package ff.ss.javaFxAuditStudio.domain.rules;

import java.util.Objects;

/**
 * Regle de gestion identifiee dans un controller JavaFX lors de l'analyse
 * de classification des responsabilites.
 *
 * @param ruleId               identifiant unique de la regle (ex. "RG-001")
 * @param description          enonce de la regle telle qu'observee dans le source
 * @param sourceRef            reference au fichier source contenant la regle
 * @param sourceLine           numero de ligne dans le fichier source (0 si inconnu)
 * @param responsibilityClass  classe de responsabilite a laquelle appartient la regle
 * @param extractionCandidate  candidat d'extraction recommande pour cette regle
 * @param uncertain            vrai si la classification necessite une validation humaine
 * @param signature            signature de methode extraite de l'AST, null si non disponible
 */
public record BusinessRule(
        String ruleId,
        String description,
        String sourceRef,
        int sourceLine,
        ResponsibilityClass responsibilityClass,
        ExtractionCandidate extractionCandidate,
        boolean uncertain,
        MethodSignature signature) {

    public BusinessRule {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(sourceRef, "sourceRef must not be null");
        Objects.requireNonNull(responsibilityClass, "responsibilityClass must not be null");
        Objects.requireNonNull(extractionCandidate, "extractionCandidate must not be null");
        // signature peut etre null (retro-compatibilite et mode regex fallback)
    }

    /**
     * Constructeur de compatibilite a 7 arguments sans signature.
     * Utilise par les tests existants et les adapters qui n'extraient pas de signature (regex fallback).
     */
    public BusinessRule(
            final String ruleId,
            final String description,
            final String sourceRef,
            final int sourceLine,
            final ResponsibilityClass responsibilityClass,
            final ExtractionCandidate extractionCandidate,
            final boolean uncertain) {
        this(ruleId, description, sourceRef, sourceLine,
                responsibilityClass, extractionCandidate, uncertain, null);
    }

    /** Retourne vrai si une signature de methode est disponible pour cette regle. */
    public boolean hasSignature() {
        return signature != null;
    }
}
