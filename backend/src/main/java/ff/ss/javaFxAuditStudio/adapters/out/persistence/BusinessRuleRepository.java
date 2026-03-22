package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository Spring Data JPA pour les regles metier extraites.
 * Les requetes JPQL agregees evitent tout chargement d'entites en memoire.
 */
public interface BusinessRuleRepository extends JpaRepository<BusinessRuleEntity, Long> {

    /**
     * Compte les regles par categorie de responsabilite pour un projet donne.
     * Passe par ClassificationResultEntity.sessionId pour rejoindre les sessions.
     *
     * @param projectId identifiant du projet (= controllerName)
     * @return liste de tableaux [responsibilityClass, count]
     */
    @Query("""
            SELECT br.responsibilityClass, COUNT(br)
            FROM BusinessRuleEntity br
            WHERE br.classification.sessionId IN (
                SELECT s.sessionId FROM AnalysisSessionEntity s WHERE s.controllerName = :projectId
            )
            GROUP BY br.responsibilityClass
            """)
    List<Object[]> countRulesByCategoryForProject(@Param("projectId") String projectId);

    /**
     * Compte les regles incertaines pour un projet donne.
     *
     * @param projectId identifiant du projet (= controllerName)
     * @return nombre de regles avec uncertain = true
     */
    @Query("""
            SELECT COUNT(br)
            FROM BusinessRuleEntity br
            WHERE br.uncertain = true
            AND br.classification.sessionId IN (
                SELECT s.sessionId FROM AnalysisSessionEntity s WHERE s.controllerName = :projectId
            )
            """)
    long countUncertainRulesForProject(@Param("projectId") String projectId);
}
