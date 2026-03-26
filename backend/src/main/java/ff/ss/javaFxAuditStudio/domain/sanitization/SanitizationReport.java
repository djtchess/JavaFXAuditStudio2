package ff.ss.javaFxAuditStudio.domain.sanitization;

import java.util.List;
import java.util.Objects;

/**
 * Rapport global de sanitisation produit par le pipeline (JAS-018).
 *
 * <p>Un rapport {@code approved} signifie qu'aucun marqueur sensible n'a ete detecte
 * apres application de toutes les regles.
 * Un rapport {@code refused} signifie qu'un marqueur subsiste et que l'envoi au LLM doit etre bloque.
 *
 * @param bundleId               Identifiant du bundle concerne
 * @param profileVersion         Version du profil de sanitisation utilise
 * @param transformations        Liste des transformations appliquees
 * @param sensitiveMarkersFound  Vrai si un marqueur sensible subsiste apres sanitisation
 * @param approved               Vrai si le bundle peut etre envoye au LLM
 */
public record SanitizationReport(
        String bundleId,
        String profileVersion,
        List<SanitizationTransformation> transformations,
        boolean sensitiveMarkersFound,
        boolean approved) {

    public SanitizationReport {
        Objects.requireNonNull(bundleId, "bundleId must not be null");
        Objects.requireNonNull(profileVersion, "profileVersion must not be null");
        transformations = (transformations != null)
                ? List.copyOf(transformations)
                : List.of();
    }

    /**
     * Cree un rapport approuve — aucun marqueur sensible detecte.
     *
     * @param bundleId        Identifiant du bundle
     * @param version         Version du profil
     * @param transformations Transformations appliquees
     * @return rapport approuve
     */
    public static SanitizationReport approved(
            final String bundleId,
            final String version,
            final List<SanitizationTransformation> transformations) {
        return new SanitizationReport(bundleId, version, transformations, false, true);
    }

    /**
     * Cree un rapport refuse — un marqueur sensible subsiste apres sanitisation.
     *
     * @param bundleId        Identifiant du bundle
     * @param version         Version du profil
     * @param transformations Transformations appliquees avant detection du marqueur
     * @return rapport refuse
     */
    public static SanitizationReport refused(
            final String bundleId,
            final String version,
            final List<SanitizationTransformation> transformations) {
        return new SanitizationReport(bundleId, version, transformations, true, false);
    }
}
