package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.time.Instant;
import java.util.Objects;

/**
 * Representation REST d'une entree d'audit de reclassification.
 *
 * @param ruleId        identifiant de la regle reclassifiee
 * @param fromCategory  categorie avant reclassification
 * @param toCategory    categorie apres reclassification
 * @param reason        raison fournie (peut etre null)
 * @param timestamp     horodatage de la reclassification
 */
public record ReclassificationAuditEntryResponse(
        String ruleId,
        String fromCategory,
        String toCategory,
        String reason,
        Instant timestamp) {

    public ReclassificationAuditEntryResponse {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        Objects.requireNonNull(fromCategory, "fromCategory must not be null");
        Objects.requireNonNull(toCategory, "toCategory must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
