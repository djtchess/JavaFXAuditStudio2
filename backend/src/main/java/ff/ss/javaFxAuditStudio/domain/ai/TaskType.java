package ff.ss.javaFxAuditStudio.domain.ai;

/**
 * Type de tâche soumise au fournisseur IA (IAP-2).
 *
 * <p>Remplace les constantes String ("NAMING", "ARTIFACT_REVIEW"...) dispersées
 * dans les services et les adapters par un enum typesafe du domaine.
 */
public enum TaskType {

    /** Suggestion de noms sémantiques pour les handlers. */
    NAMING,

    /** Génération de descriptions fonctionnelles. */
    DESCRIPTION,

    /** Aide à la classification des responsabilités. */
    CLASSIFICATION_HINT,

    /** Revue qualité des artefacts générés. */
    ARTIFACT_REVIEW,

    /** Génération des classes cibles Spring Boot. */
    SPRING_BOOT_GENERATION;

    /**
     * Convertit une chaîne en {@link TaskType} (insensible à la casse).
     *
     * @param value valeur à convertir (ex. "NAMING", "naming")
     * @return le TaskType correspondant
     * @throws IllegalArgumentException si la valeur est inconnue ou null
     */
    public static TaskType fromString(final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TaskType ne peut pas être vide ou null");
        }
        try {
            return TaskType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("TaskType inconnu : '" + value
                    + "'. Valeurs acceptées : NAMING, DESCRIPTION, CLASSIFICATION_HINT, "
                    + "ARTIFACT_REVIEW, SPRING_BOOT_GENERATION");
        }
    }
}
