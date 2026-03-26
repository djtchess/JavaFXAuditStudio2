package ff.ss.javaFxAuditStudio.domain.ai;

import java.util.Objects;

import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationReport;

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
 * @param report                Rapport de sanitisation produit par le pipeline (null si pipeline absent)
 */
public record SanitizedBundle(
        String bundleId,
        String controllerRef,
        String sanitizedSource,
        int estimatedTokens,
        String sanitizationVersion,
        SanitizationReport report) {

    public SanitizedBundle {
        Objects.requireNonNull(bundleId, "bundleId must not be null");
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        Objects.requireNonNull(sanitizedSource, "sanitizedSource must not be null");
        Objects.requireNonNull(sanitizationVersion, "sanitizationVersion must not be null");
        if (estimatedTokens < 0) {
            throw new IllegalArgumentException("estimatedTokens must be >= 0");
        }
        // report is nullable : absent when pipeline is disabled or in fallback mode
    }
}
