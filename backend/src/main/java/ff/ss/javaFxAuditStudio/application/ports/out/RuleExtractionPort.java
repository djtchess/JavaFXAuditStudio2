package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;

import java.util.List;

/**
 * Port sortant : extraction des règles de gestion depuis le contenu source d'un controller.
 */
public interface RuleExtractionPort {

    /**
     * Extrait les règles de gestion depuis le contenu Java ou FXML fourni.
     *
     * @param controllerRef référence du controller (chemin ou identifiant)
     * @param javaContent   contenu textuel du fichier Java source (vide si non disponible)
     * @return liste des règles extraites, jamais null
     */
    List<BusinessRule> extract(String controllerRef, String javaContent);
}
