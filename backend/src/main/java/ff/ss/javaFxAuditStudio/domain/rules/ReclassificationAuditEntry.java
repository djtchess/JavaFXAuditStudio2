package ff.ss.javaFxAuditStudio.domain.rules;

import java.time.Instant;
import java.util.Objects;

/**
 * Entree d'audit pour une reclassification manuelle d'une regle de gestion.
 * Record immuable du domaine : pas de dependance Spring, JPA ou DTO.
 *
 * @param auditId       identifiant unique de l'entree d'audit (UUID en chaine)
 * @param analysisId    identifiant de la session d'analyse concernee
 * @param ruleId        identifiant de la regle reclassifiee
 * @param fromCategory  categorie de responsabilite avant reclassification
 * @param toCategory    categorie de responsabilite apres reclassification
 * @param reason        raison fournie par l'utilisateur (peut etre null)
 * @param timestamp     horodatage de la reclassification
 */
public record ReclassificationAuditEntry(
        String auditId,
        String analysisId,
        String ruleId,
        ResponsibilityClass fromCategory,
        ResponsibilityClass toCategory,
        String reason,
        Instant timestamp) {

    public ReclassificationAuditEntry {
        Objects.requireNonNull(auditId, "auditId must not be null");
        Objects.requireNonNull(analysisId, "analysisId must not be null");
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        Objects.requireNonNull(fromCategory, "fromCategory must not be null");
        Objects.requireNonNull(toCategory, "toCategory must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        // reason peut etre null
    }
}
