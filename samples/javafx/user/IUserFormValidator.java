package com.example.user.validation;

import com.example.user.dto.UserFormData;

/**
 * Contrat de validation du formulaire utilisateur.
 *
 * <p>Sépare la définition du contrat de son implémentation, conformément au
 * principe d'inversion de dépendances (DIP). Facilite le mocking en tests.</p>
 */
public interface IUserFormValidator {

    /**
     * Valide l'ensemble des données du formulaire.
     *
     * @param form les données saisies — ne doit pas être {@code null}.
     * @return un {@link ValidationResult} décrivant les erreurs trouvées,
     *         ou un résultat valide si toutes les règles sont respectées.
     */
    ValidationResult validate(UserFormData form);

    /**
     * Valide uniquement l'adresse email (validation à la saisie).
     *
     * @param email l'adresse à vérifier.
     * @return {@code true} si l'email est syntaxiquement valide.
     */
    boolean isEmailValide(String email);

    /**
     * Valide uniquement le mot de passe (validation à la saisie).
     *
     * @param password le mot de passe à vérifier.
     * @return {@code true} si le mot de passe respecte la politique de sécurité.
     */
    boolean isPasswordValide(String password);

    /**
     * Vérifie la cohérence entre le mot de passe et sa confirmation.
     *
     * @param password        le mot de passe initial.
     * @param passwordConfirm la confirmation saisie.
     * @return {@code true} si les deux valeurs sont identiques et non vides.
     */
    boolean isPasswordConfirmValide(String password, String passwordConfirm);
}
