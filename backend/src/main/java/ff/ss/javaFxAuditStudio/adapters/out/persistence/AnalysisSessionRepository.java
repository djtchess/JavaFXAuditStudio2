package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnalysisSessionRepository extends JpaRepository<AnalysisSessionEntity, String> {

    @Query("SELECT COUNT(s) FROM AnalysisSessionEntity s WHERE s.controllerName = :projectId")
    long countByControllerName(@Param("projectId") String projectId);

    @Query("SELECT COUNT(s) FROM AnalysisSessionEntity s WHERE s.controllerName = :projectId AND s.status = :status")
    long countByControllerNameAndStatus(@Param("projectId") String projectId, @Param("status") String status);

    @Query("SELECT COUNT(s) FROM AnalysisSessionEntity s WHERE s.controllerName = :projectId AND s.status IN :statuses")
    long countByControllerNameAndStatusIn(
            @Param("projectId") String projectId,
            @Param("statuses") List<String> statuses);

    @Query("SELECT DISTINCT s.controllerName FROM AnalysisSessionEntity s ORDER BY s.controllerName")
    List<String> findDistinctControllerNames();
}
