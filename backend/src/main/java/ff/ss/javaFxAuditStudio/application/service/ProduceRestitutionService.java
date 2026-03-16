package ff.ss.javaFxAuditStudio.application.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import ff.ss.javaFxAuditStudio.application.ports.in.ProduceRestitutionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.RestitutionFormatterPort;
import ff.ss.javaFxAuditStudio.application.ports.out.RestitutionPersistencePort;
import ff.ss.javaFxAuditStudio.domain.restitution.ConfidenceLevel;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionSummary;

/**
 * Service applicatif qui orchestre la production d'un rapport de restitution.
 * La connexion reelle aux use cases de cartographie, classification, migration
 * et generation est prevue dans un lot ulterieur.
 * En l'etat, le rapport produit est un stub avec niveau de confiance INSUFFICIENT.
 */
public final class ProduceRestitutionService implements ProduceRestitutionUseCase {

    private static final String STUB_FINDING = "Restitution stub - moteur non encore connecte";

    private final RestitutionFormatterPort restitutionFormatterPort;
    private final RestitutionPersistencePort restitutionPersistencePort;

    public ProduceRestitutionService(
            final RestitutionFormatterPort restitutionFormatterPort,
            final RestitutionPersistencePort restitutionPersistencePort) {
        this.restitutionFormatterPort = Objects.requireNonNull(
                restitutionFormatterPort, "restitutionFormatterPort must not be null");
        this.restitutionPersistencePort = Objects.requireNonNull(
                restitutionPersistencePort, "restitutionPersistencePort must not be null");
    }

    @Override
    public RestitutionReport handle(final String sessionId, final String controllerRef) {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");

        Optional<RestitutionReport> cached = restitutionPersistencePort.findBySessionId(sessionId);
        if (cached.isPresent()) {
            return cached.get();
        }

        RestitutionSummary summary = new RestitutionSummary(
                controllerRef,
                0,
                0,
                0,
                0,
                ConfidenceLevel.INSUFFICIENT,
                false);

        RestitutionReport report = new RestitutionReport(
                summary,
                List.of(),
                List.of(),
                List.of(STUB_FINDING));

        restitutionPersistencePort.save(sessionId, report);
        return report;
    }
}
