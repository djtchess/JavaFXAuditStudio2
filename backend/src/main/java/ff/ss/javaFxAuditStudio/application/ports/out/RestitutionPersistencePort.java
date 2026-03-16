package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;

import java.util.Optional;

/**
 * Port sortant pour la persistence des rapports de restitution.
 */
public interface RestitutionPersistencePort {

    RestitutionReport save(String sessionId, RestitutionReport report);

    Optional<RestitutionReport> findBySessionId(String sessionId);
}
