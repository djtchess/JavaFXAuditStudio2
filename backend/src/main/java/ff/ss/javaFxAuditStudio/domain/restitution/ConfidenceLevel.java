package ff.ss.javaFxAuditStudio.domain.restitution;

/**
 * Niveau de confiance global attribue a une restitution.
 * HIGH    : analyse complete, aucune incertitude bloquante.
 * MEDIUM  : quelques inconnues mineures, restitution exploitable.
 * LOW     : incertitudes significatives, relecture humaine requise.
 * INSUFFICIENT : donnees insuffisantes, restitution non actionnable.
 */
public enum ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW,
    INSUFFICIENT
}
