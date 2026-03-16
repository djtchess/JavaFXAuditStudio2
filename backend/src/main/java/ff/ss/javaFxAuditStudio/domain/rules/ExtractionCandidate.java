package ff.ss.javaFxAuditStudio.domain.rules;

/**
 * Type de candidat d'extraction identifié lors de la classification
 * des responsabilités d'un controller JavaFX.
 * Chaque valeur désigne la cible architecturale recommandée pour une règle donnée.
 */
public enum ExtractionCandidate {

    /** Règle métier stable, décision, statut ou éligibilité → à extraire en Policy. */
    POLICY,

    /** Intention utilisateur orchestrant plusieurs services → à extraire en UseCase. */
    USE_CASE,

    /** Appel technique externe (REST, fichier, matériel, impression) → à encapsuler en Gateway. */
    GATEWAY,

    /** État de présentation (sélection, modes, flags, messages) → à porter par un ViewModel. */
    VIEW_MODEL,

    /** Construction d'objets métier depuis les champs UI → à déléguer à un Assembler. */
    ASSEMBLER,

    /** Variante de workflow conditionnelle substantielle → à modéliser en Strategy. */
    STRATEGY,

    /** Aucune extraction recommandée — la zone reste dans le controller ou est déjà correcte. */
    NONE
}
