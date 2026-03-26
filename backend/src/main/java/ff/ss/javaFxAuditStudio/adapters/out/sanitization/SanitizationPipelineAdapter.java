package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.configuration.SanitizationProperties;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TokenEstimator;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRefusedException;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationReport;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationTransformation;

/**
 * Orchestrateur du pipeline de sanitisation (JAS-018).
 *
 * <p>Applique les sanitizers en sequence, verifie l'absence de marqueurs residuels
 * et controle le plafond de tokens avant de retourner le bundle pret a etre envoye au LLM.
 *
 * <p>Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration}.
 */
public class SanitizationPipelineAdapter implements SanitizationPort {

    private static final Logger LOG = LoggerFactory.getLogger(SanitizationPipelineAdapter.class);

    private final List<Sanitizer> sanitizers;
    private final SensitiveMarkerDetector detector;
    private final SanitizationProperties properties;

    public SanitizationPipelineAdapter(
            final List<Sanitizer> sanitizers,
            final SensitiveMarkerDetector detector,
            final SanitizationProperties properties) {
        this.sanitizers = List.copyOf(
                Objects.requireNonNull(sanitizers, "sanitizers must not be null"));
        this.detector = Objects.requireNonNull(detector, "detector must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public SanitizedBundle sanitize(
            final String bundleId,
            final String rawSource,
            final String controllerRef) {
        Objects.requireNonNull(bundleId, "bundleId must not be null");
        Objects.requireNonNull(rawSource, "rawSource must not be null");
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");
        if (rawSource.isBlank()) {
            throw new IllegalArgumentException(
                    "rawSource must not be blank for bundle " + bundleId);
        }

        String current = rawSource;
        List<SanitizationTransformation> transformations = new ArrayList<>();

        for (Sanitizer sanitizer : sanitizers) {
            current = sanitizer.apply(current);
            SanitizationTransformation t = sanitizer.report();
            transformations.add(t);
            LOG.debug("Sanitizer {} applied: {} occurrences", t.ruleType(), t.occurrenceCount());
        }

        if (detector.hasSensitiveMarkers(current)) {
            SanitizationReport report = SanitizationReport.refused(
                    bundleId, properties.profileVersion(), transformations);
            LOG.warn("Sanitisation refusee pour bundle {} : marqueur sensible residuel detecte",
                    bundleId);
            throw new SanitizationRefusedException(
                    "Sanitisation refusee : marqueur sensible detecte apres pipeline pour bundle "
                    + bundleId);
        }

        int estimatedTokens = TokenEstimator.estimate(current);
        if (estimatedTokens > properties.maxTokens()) {
            LOG.warn("Sanitisation refusee pour bundle {} : {} tokens > plafond {}",
                    bundleId, estimatedTokens, properties.maxTokens());
            throw new SanitizationRefusedException(
                    "Sanitisation refusee : token count " + estimatedTokens
                    + " depasse le plafond " + properties.maxTokens()
                    + " pour bundle " + bundleId);
        }

        SanitizationReport report = SanitizationReport.approved(
                bundleId, properties.profileVersion(), transformations);
        int totalOccurrences = transformations.stream()
                .mapToInt(SanitizationTransformation::occurrenceCount).sum();
        LOG.info("Sanitisation approuvee pour bundle {} : {} transformations, {} tokens estimes",
                bundleId, totalOccurrences, estimatedTokens);

        return new SanitizedBundle(
                bundleId,
                controllerRef,
                current,
                estimatedTokens,
                properties.profileVersion(),
                report);
    }

    /**
     * Mode dry-run : applique les sanitizers en sequence, collecte les transformations
     * et observe la detection de marqueurs sensibles sans jamais lever de
     * {@link SanitizationRefusedException}.
     *
     * <p>Cette methode ne transmet jamais la source au LLM.
     */
    @Override
    public SanitizationReport previewTransformations(
            final String bundleId,
            final String rawSource,
            final String controllerRef) {
        Objects.requireNonNull(bundleId, "bundleId must not be null");
        Objects.requireNonNull(rawSource, "rawSource must not be null");
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");

        String current = rawSource;
        List<SanitizationTransformation> transformations = new ArrayList<>();

        for (Sanitizer sanitizer : sanitizers) {
            current = sanitizer.apply(current);
            SanitizationTransformation t = sanitizer.report();
            transformations.add(t);
            LOG.debug("DryRun sanitizer {} applied: {} occurrences", t.ruleType(), t.occurrenceCount());
        }

        boolean sensitiveRemains = detector.hasSensitiveMarkers(current);
        LOG.debug("DryRun bundle {} : sensitiveMarkersFound={}", bundleId, sensitiveRemains);

        if (sensitiveRemains) {
            return SanitizationReport.refused(bundleId, properties.profileVersion(), transformations);
        }
        return SanitizationReport.approved(bundleId, properties.profileVersion(), transformations);
    }
}
