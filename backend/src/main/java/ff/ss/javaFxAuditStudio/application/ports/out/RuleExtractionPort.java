package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.rules.ExtractionResult;

/**
 * Port sortant : extraction des regles de gestion depuis le contenu source d'un controller.
 */
public interface RuleExtractionPort {

    /**
     * Extrait les regles de gestion depuis le contenu Java ou FXML fourni.
     *
     * @param controllerRef reference du controller (chemin ou identifiant)
     * @param javaContent   contenu textuel du fichier Java source (vide si non disponible)
     * @return resultat de l'extraction contenant les regles et le mode de parsing utilise, jamais null
     */
    ExtractionResult extract(String controllerRef, String javaContent);
}
