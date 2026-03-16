package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;

/**
 * Port entrant : demande de classification des responsabilités d'un controller.
 */
public interface ClassifyResponsibilitiesUseCase {

    /**
     * Classifie les responsabilités et les règles de gestion du controller désigné.
     *
     * @param controllerRef référence du controller à classifier (chemin ou identifiant)
     * @return résultat de classification contenant les règles certaines et incertaines
     */
    ClassificationResult handle(String controllerRef);
}
