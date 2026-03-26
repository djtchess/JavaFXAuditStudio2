package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.application.ports.out.MultiFileSanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.TokenEstimator;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizableFile;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationReport;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationTransformation;

/**
 * Adapter de sanitisation multi-fichiers (QW-5).
 *
 * <p>Implémente {@link MultiFileSanitizationPort} en s'appuyant sur :
 * <ul>
 *   <li>{@link SanitizationPort} pour les fichiers Java (pipeline complet)</li>
 *   <li>{@link SecretSanitizer} + {@link SemgrepScanSanitizer} pour les fichiers non-Java</li>
 * </ul>
 *
 * <p>Les sanitizers AST ({@code OpenRewriteIdentifierSanitizer}) et regex de classes Java
 * ({@code IdentifierSanitizer}) ne sont jamais appliques aux fichiers non-Java.
 *
 * <p>Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration}.
 */
public class MultiFileSanitizationAdapter implements MultiFileSanitizationPort {

    private static final Logger LOG = LoggerFactory.getLogger(MultiFileSanitizationAdapter.class);

    private static final String SANITIZATION_VERSION = "multi-1.0";

    private final SanitizationPort javaDelegate;
    private final SecretSanitizer secretSanitizer;
    private final SemgrepScanSanitizer semgrepScanSanitizer;

    public MultiFileSanitizationAdapter(
            final SanitizationPort javaDelegate,
            final SecretSanitizer secretSanitizer,
            final SemgrepScanSanitizer semgrepScanSanitizer) {
        this.javaDelegate = Objects.requireNonNull(javaDelegate, "javaDelegate must not be null");
        this.secretSanitizer = Objects.requireNonNull(secretSanitizer, "secretSanitizer must not be null");
        this.semgrepScanSanitizer = Objects.requireNonNull(semgrepScanSanitizer, "semgrepScanSanitizer must not be null");
    }

    @Override
    public List<SanitizedBundle> sanitizeFiles(
            final String bundleId,
            final List<SanitizableFile> files) {
        Objects.requireNonNull(bundleId, "bundleId must not be null");
        Objects.requireNonNull(files, "files must not be null");

        if (files.isEmpty()) {
            LOG.debug("MultiFileSanitizationAdapter : aucun fichier a sanitiser pour bundle {}", bundleId);
            return List.of();
        }

        List<SanitizedBundle> results = new ArrayList<>(files.size());
        for (SanitizableFile file : files) {
            SanitizedBundle bundle = sanitizeOne(bundleId, file);
            results.add(bundle);
        }
        return List.copyOf(results);
    }

    private SanitizedBundle sanitizeOne(final String bundleId, final SanitizableFile file) {
        if (file.isJava()) {
            LOG.debug("Delegation sanitisation Java pour {}", file.fileName());
            return javaDelegate.sanitize(bundleId, file.content(), file.fileName());
        }
        return sanitizeNonJava(bundleId, file);
    }

    private SanitizedBundle sanitizeNonJava(final String bundleId, final SanitizableFile file) {
        LOG.debug("Sanitisation non-Java ({}) pour {}", file.fileType(), file.fileName());

        String current = secretSanitizer.apply(file.content());
        SanitizationTransformation secretTransfo = secretSanitizer.report();

        current = semgrepScanSanitizer.applyToFile(new SanitizableFile(file.fileName(), current, file.fileType()));
        SanitizationTransformation semgrepTransfo = semgrepScanSanitizer.report();

        List<SanitizationTransformation> transformations = List.of(secretTransfo, semgrepTransfo);
        SanitizationReport report = SanitizationReport.approved(bundleId, SANITIZATION_VERSION, transformations);
        int estimatedTokens = TokenEstimator.estimate(current);

        LOG.info("Sanitisation non-Java approuvee pour {} (bundle {}) : {} tokens estimes",
                file.fileName(), bundleId, estimatedTokens);

        return new SanitizedBundle(bundleId, file.fileName(), current, estimatedTokens, SANITIZATION_VERSION, report);
    }
}
