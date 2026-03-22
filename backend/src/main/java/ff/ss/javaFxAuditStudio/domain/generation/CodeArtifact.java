package ff.ss.javaFxAuditStudio.domain.generation;

import ff.ss.javaFxAuditStudio.application.generation.ArtifactResult;

import java.util.List;
import java.util.Objects;

/**
 * Artefact de code genere, enrichi des avertissements de validation (JAS-009).
 * Implementee ArtifactResult (JAS-010) pour permettre le typage unifie des sorties de generation.
 *
 * <p>Constructeur a 6 arguments conserve pour la compatibilite avec les generateurs
 * existants : initialise {@code generationWarnings} a vide et
 * {@code generationStatus} a "OK".
 */
public record CodeArtifact(
        String artifactId,
        ArtifactType type,
        int lotNumber,
        String className,
        String content,
        boolean transitionalBridge,
        List<ArtifactValidationWarning> generationWarnings,
        String generationStatus
) implements ArtifactResult {
    public CodeArtifact {
        Objects.requireNonNull(artifactId, "artifactId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(className, "className must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (lotNumber < 1 || lotNumber > 5) {
            throw new IllegalArgumentException("lotNumber must be between 1 and 5, got: " + lotNumber);
        }
        generationWarnings = (generationWarnings != null) ? List.copyOf(generationWarnings) : List.of();
        generationStatus = (generationStatus != null && !generationStatus.isBlank())
                ? generationStatus
                : (generationWarnings == null || generationWarnings.isEmpty() ? "OK" : "WARNING");
    }

    /**
     * Constructeur de compatibilite a 6 arguments (avant JAS-009).
     * Initialise les avertissements a vide et le statut a "OK".
     */
    public CodeArtifact(
            final String artifactId,
            final ArtifactType type,
            final int lotNumber,
            final String className,
            final String content,
            final boolean transitionalBridge) {
        this(artifactId, type, lotNumber, className, content, transitionalBridge, List.of(), "OK");
    }
}
