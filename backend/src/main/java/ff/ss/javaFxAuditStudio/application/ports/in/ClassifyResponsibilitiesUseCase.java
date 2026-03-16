package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;

/**
 * Port entrant : demande de classification des responsabilites d'un controller.
 */
public interface ClassifyResponsibilitiesUseCase {

    /**
     * Classifie les responsabilites et les regles de gestion du controller designe.
     *
     * @param sessionId     identifiant de la session d'analyse
     * @param controllerRef reference du controller a classifier (chemin ou identifiant)
     * @return resultat de classification contenant les regles certaines et incertaines
     */
    ClassificationResult handle(String sessionId, String controllerRef);
}
