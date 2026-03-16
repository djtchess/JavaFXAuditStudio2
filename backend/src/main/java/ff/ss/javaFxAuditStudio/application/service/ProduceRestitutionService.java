package ff.ss.javaFxAuditStudio.application.service;

import java.util.List;
import java.util.Objects;

import ff.ss.javaFxAuditStudio.application.ports.in.ProduceRestitutionUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.RestitutionFormatterPort;
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

    public ProduceRestitutionService(final RestitutionFormatterPort restitutionFormatterPort) {
        Objects.requireNonNull(restitutionFormatterPort, "restitutionFormatterPort must not be null");
        this.restitutionFormatterPort = restitutionFormatterPort;
    }

    @Override
    public RestitutionReport handle(final String controllerRef) {
        Objects.requireNonNull(controllerRef, "controllerRef must not be null");

        RestitutionSummary summary;
        RestitutionReport report;

        summary = new RestitutionSummary(
                controllerRef,
                0,
                0,
                0,
                0,
                ConfidenceLevel.INSUFFICIENT,
                false);

        report = new RestitutionReport(
                summary,
                List.of(),
                List.of(),
                List.of(STUB_FINDING));

        return report;
    }
}
