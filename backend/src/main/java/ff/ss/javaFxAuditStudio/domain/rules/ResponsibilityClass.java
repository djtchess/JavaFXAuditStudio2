package ff.ss.javaFxAuditStudio.domain.rules;

/**
 * Classe de responsabilité identifiée dans un controller JavaFX.
 * Chaque valeur représente une couche de l'architecture cible vers laquelle
 * une règle ou une zone de code doit être déplacée.
 */
public enum ResponsibilityClass {

    UI("Liaison directe avec les composants FXML et les événements graphiques"),
    PRESENTATION("État de présentation : sélection, modes écran, flags boutons, messages utilisateur"),
    APPLICATION("Orchestration applicative : use cases, coordination de flux, intentions utilisateur"),
    BUSINESS("Décision métier stable : policy, statut, éligibilité, règle de transition"),
    TECHNICAL("Accès technique : REST, fichiers, impression, matériel, lancement d'outils"),
    UNKNOWN("Responsabilité non déterminée — nécessite une analyse complémentaire");

    private final String description;

    ResponsibilityClass(final String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
