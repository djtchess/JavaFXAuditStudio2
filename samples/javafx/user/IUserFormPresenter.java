package com.example.user.ui.presenter;

/**
 * Contrat du presenter du formulaire utilisateur.
 *
 * <p>Le presenter orchestre les actions déclenchées par l'UI.
 * Il ne connaît aucun composant JavaFX : il lit et écrit uniquement dans
 * {@link com.example.user.ui.state.UserFormState}.</p>
 */
public interface IUserFormPresenter {

    /**
     * Tente de valider et d'enregistrer l'utilisateur à partir de l'état courant.
     * Met à jour l'état (erreurs, succès, indicateur de chargement) en retour.
     */
    void enregistrer();

    /**
     * Réinitialise le formulaire à son état initial.
     */
    void reinitialiser();

    /**
     * Déclenche la validation temps-réel de l'email.
     *
     * @param email la valeur courante du champ email.
     * @return {@code true} si l'email est valide.
     */
    boolean validerEmail(String email);

    /**
     * Déclenche la validation temps-réel du mot de passe.
     *
     * @param password la valeur courante.
     * @return {@code true} si le mot de passe est valide.
     */
    boolean validerPassword(String password);

    /**
     * Déclenche la validation temps-réel de la confirmation du mot de passe.
     *
     * @param password        le mot de passe initial.
     * @param passwordConfirm la confirmation saisie.
     * @return {@code true} si les deux valeurs correspondent.
     */
    boolean validerPasswordConfirm(String password, String passwordConfirm);

    /**
     * Vérifie qu'un champ texte obligatoire est renseigné.
     *
     * @param value valeur du champ.
     * @return {@code true} si la valeur n'est ni nulle ni vide après trim.
     */
    boolean validerChampObligatoire(String value);

    /**
     * Vérifie qu'une sélection obligatoire est présente.
     *
     * @param value valeur sélectionnée.
     * @return {@code true} si une sélection existe.
     */
    boolean validerSelectionObligatoire(Object value);
}
