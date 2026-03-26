package ff.ss.javaFxAuditStudio.domain.ai;

import java.util.Objects;

/**
 * Bundle sanitise transmis au fournisseur IA.
 *
 * <p>Le code source inclus doit avoir ete desensibilise avant construction.
 * Ne jamais loguer le champ {@code sanitizedSource}.
 *
 * @param bundleId              UUID de tracabilite
 * @param controllerRef         Reference au controller sanitise
 * @param sanitizedSource       Code source deja desensibilise
 * @param estimatedTokens       Estimation tokens avant envoi
 * @param sanitizationVersion   Version du pipeline de desensibilisation
 */
public record SanitizedBundle(
        String bundleId,
        String controllerRef,
        String sanitizedSource,
        int estimatedTokens,
        String sanitizationVersion) {

    public SanitizedBundle {
        Objects.requireNonNull(bundleId, "bundleId must not be null");
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(sanitizedSource, "sanitizedSource must not be null");
        Objects.requireNonNull(sanitizationVersion, "sanitizationVersion must not be null");
        if (estimatedTokens < 0) {
            throw new IllegalArgumentException("estimatedTokens must be >= 0");
        }
    }
}
