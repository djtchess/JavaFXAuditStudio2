package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Entree d'audit de reclassification")
public record ReclassificationAuditEntryResponse(
        @Schema(description = "Identifiant de la regle reclassifiee")
        String ruleId,
        @Schema(description = "Categorie avant la reclassification")
        String fromCategory,
        @Schema(description = "Categorie apres la reclassification")
        String toCategory,
        @Schema(description = "Justification fournie par l'utilisateur, peut etre null", nullable = true)
        String reason,
        @Schema(description = "Horodatage de la reclassification (ISO-8601)")
        Instant timestamp) {

    public ReclassificationAuditEntryResponse {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        Objects.requireNonNull(fromCategory, "fromCategory must not be null");
        Objects.requireNonNull(toCategory, "toCategory must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
