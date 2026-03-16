package com.example.user.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Résultat structuré d'une validation de formulaire.
 *
 * <p>Chaque erreur est associée à un champ nommé, ce qui permet à la couche UI
 * de colorer précisément le composant concerné sans embarquer de logique métier.</p>
 *
 * <p>Exemple d'utilisation :</p>
 * <pre>{@code
 * ValidationResult result = validator.validate(formData);
 * if (!result.isValid()) {
 *     result.getFieldErrors().forEach(e -> marquerErreur(e.getField()));
 *     afficherMessages(result.getMessages());
 * }
 * }</pre>
 */
public final class ValidationResult {

    // -------------------------------------------------------------------------
    // Constantes — noms de champs (évite les chaînes magiques dans les tests)
    // -------------------------------------------------------------------------

    public static final String FIELD_CIVILITE         = "civilite";
    public static final String FIELD_NOM              = "nom";
    public static final String FIELD_PRENOM           = "prenom";
    public static final String FIELD_LOGIN            = "login";
    public static final String FIELD_EMAIL            = "email";
    public static final String FIELD_PASSWORD         = "password";
    public static final String FIELD_PASSWORD_CONFIRM = "passwordConfirm";
    public static final String FIELD_ROLE             = "role";

    // -------------------------------------------------------------------------
    // Erreur unitaire (champ + message)
    // -------------------------------------------------------------------------

    public static final class FieldError {

        private final String field;
        private final String message;

        public FieldError(final String field, final String message) {
            this.field   = field;
            this.message = message;
        }

        public String getField()   { return field; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return field + ": " + message;
        }
    }

    // -------------------------------------------------------------------------
    // État interne
    // -------------------------------------------------------------------------

    private final List<FieldError> errors = new ArrayList<>();

    // -------------------------------------------------------------------------
    // API publique (lecture seule pour les consommateurs)
    // -------------------------------------------------------------------------

    /**
     * Indique si la validation est passée sans erreur.
     *
     * @return {@code true} si aucune erreur n'a été enregistrée.
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Retourne la liste non modifiable de toutes les erreurs.
     *
     * @return liste des erreurs (peut être vide, jamais {@code null}).
     */
    public List<FieldError> getFieldErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Retourne uniquement les messages d'erreur (sans les noms de champs).
     *
     * @return liste de messages humainement lisibles.
     */
    public List<String> getMessages() {
        return errors.stream()
                .map(FieldError::getMessage)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Retourne les noms des champs en erreur.
     *
     * @return liste des identifiants de champs.
     */
    public List<String> getFieldsInError() {
        return errors.stream()
                .map(FieldError::getField)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Vérifie si un champ particulier est en erreur.
     *
     * @param field le nom du champ à vérifier.
     * @return {@code true} si au moins une erreur concerne ce champ.
     */
    public boolean hasErrorOnField(final String field) {
        return errors.stream().anyMatch(e -> e.getField().equals(field));
    }

    // -------------------------------------------------------------------------
    // API d'alimentation (package-private : seul le validator écrit dans ce résultat)
    // -------------------------------------------------------------------------

    void addError(final String field, final String message) {
        errors.add(new FieldError(field, message));
    }

    @Override
    public String toString() {
        if (isValid()) {
            return "ValidationResult{valid}";
        }
        return "ValidationResult{errors=" + errors + '}';
    }
}
