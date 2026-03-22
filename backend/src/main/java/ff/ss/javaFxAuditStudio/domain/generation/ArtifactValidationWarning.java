package ff.ss.javaFxAuditStudio.domain.generation;

/**
 * JAS-009 — Avertissements de validation structurelle d'un artefact genere.
 *
 * <p>Ces valeurs sont produites par le validateur technique (adapter) et stockees
 * dans le domaine sans dependance externe. Elles permettent au frontend d'afficher
 * des indicateurs de qualite sur les artefacts generes.
 */
public enum ArtifactValidationWarning {

    /** Deux methodes ou plus partagent le meme nom dans l'artefact. */
    DUPLICATE_METHOD_NAME,

    /** Au moins un import semble manquant au regard des types utilises. */
    MISSING_IMPORT,

    /** L'artefact ne contient aucune methode significative. */
    EMPTY_BODY,

    /** La syntaxe Java de l'artefact n'a pas pu etre parsee avec succes. */
    PARSE_ERROR
}
