package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestitutionReportRepository extends JpaRepository<RestitutionReportEntity, Long> {

    Optional<RestitutionReportEntity> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
