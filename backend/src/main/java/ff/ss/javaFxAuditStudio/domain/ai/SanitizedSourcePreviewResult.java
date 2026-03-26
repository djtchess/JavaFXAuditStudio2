package ff.ss.javaFxAuditStudio.domain.ai;

import java.util.Objects;

/**
 * Resultat de previsualisation du code sanitise (JAS-031).
 *
 * <p>Enrichit {@link SanitizedBundle} avec un indicateur de sanitisation effective,
 * distinguant un passage reel dans le pipeline d'un retour brut en mode fallback.
 *
 * @param bundle    Bundle sanitise (ou brut si fallback)
 * @param sanitized Vrai si le pipeline de sanitisation a ete applique
 */
public record SanitizedSourcePreviewResult(
        SanitizedBundle bundle,
        boolean sanitized) {

    public SanitizedSourcePreviewResult {
        Objects.requireNonNull(bundle, "bundle must not be null");
    }
}
