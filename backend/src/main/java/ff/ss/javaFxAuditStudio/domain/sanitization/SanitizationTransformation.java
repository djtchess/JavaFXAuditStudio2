package ff.ss.javaFxAuditStudio.domain.sanitization;

import java.util.Objects;

/**
 * Rapport d'une transformation de sanitisation (JAS-018).
 *
 * <p>Ne contient jamais la valeur d'origine pour eviter toute fuite de donnee sensible dans les logs.
 *
 * @param ruleType        Type de regle appliquee
 * @param occurrenceCount Nombre d'occurrences remplacees ou supprimees
 * @param description     Description courte de la transformation (sans donnee sensible)
 */
public record SanitizationTransformation(
        SanitizationRuleType ruleType,
        int occurrenceCount,
        String description) {

    public SanitizationTransformation {
        Objects.requireNonNull(ruleType, "ruleType must not be null");
        Objects.requireNonNull(description, "description must not be null");
        if (occurrenceCount < 0) {
            throw new IllegalArgumentException("occurrenceCount must be >= 0");
        }
    }
}
