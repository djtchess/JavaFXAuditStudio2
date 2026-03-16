package com.example.user.validation;

import com.example.user.dto.UserFormData;
import com.example.user.i18n.MessageProvider;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.example.user.validation.ValidationResult.*;

/**
 * Implémentation des règles de validation du formulaire utilisateur.
 *
 * <p>Chaque règle est une méthode privée isolée pour faciliter la lecture,
 * la modification et les tests unitaires ciblés.</p>
 *
 * <p>Les messages sont délégués à {@link MessageProvider} pour l'i18n.</p>
 */
@Component
public class UserFormValidator implements IUserFormValidator {

    // -------------------------------------------------------------------------
    // Constantes de règles métier
    // -------------------------------------------------------------------------

    public static final int    PASSWORD_MIN_LENGTH = 8;
    public static final String EMAIL_REGEX =
            "^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$";

    // -------------------------------------------------------------------------
    // Dépendances
    // -------------------------------------------------------------------------

    private final MessageProvider messages;

    public UserFormValidator(final MessageProvider messages) {
        this.messages = Objects.requireNonNull(messages, "MessageProvider est obligatoire.");
    }

    // -------------------------------------------------------------------------
    // Validation globale
    // -------------------------------------------------------------------------

    @Override
    public ValidationResult validate(final UserFormData form) {
        Objects.requireNonNull(form, "Le formulaire ne peut pas être null.");

        final ValidationResult result = new ValidationResult();

        validerCivilite(form, result);
        validerNom(form, result);
        validerPrenom(form, result);
        validerLogin(form, result);
        validerEmail(form, result);
        validerPassword(form, result);
        validerPasswordConfirmation(form, result);
        validerRole(form, result);

        return result;
    }

    // -------------------------------------------------------------------------
    // Validations à la saisie (exposées par l'interface)
    // -------------------------------------------------------------------------

    @Override
    public boolean isEmailValide(final String email) {
        return email != null && !email.isBlank() && email.matches(EMAIL_REGEX);
    }

    @Override
    public boolean isPasswordValide(final String password) {
        return password != null && password.length() >= PASSWORD_MIN_LENGTH;
    }

    @Override
    public boolean isPasswordConfirmValide(final String password, final String passwordConfirm) {
        return password != null
                && passwordConfirm != null
                && !passwordConfirm.isBlank()
                && password.equals(passwordConfirm);
    }

    // -------------------------------------------------------------------------
    // Règles individuelles (privées)
    // -------------------------------------------------------------------------

    private void validerCivilite(final UserFormData form, final ValidationResult result) {
        if (form.getCivilite() == null) {
            result.addError(FIELD_CIVILITE, messages.get("validation.civilite.required"));
        }
    }

    private void validerNom(final UserFormData form, final ValidationResult result) {
        if (form.getNom().isBlank()) {
            result.addError(FIELD_NOM, messages.get("validation.nom.required"));
        }
    }

    private void validerPrenom(final UserFormData form, final ValidationResult result) {
        if (form.getPrenom().isBlank()) {
            result.addError(FIELD_PRENOM, messages.get("validation.prenom.required"));
        }
    }

    private void validerLogin(final UserFormData form, final ValidationResult result) {
        if (form.getLogin().isBlank()) {
            result.addError(FIELD_LOGIN, messages.get("validation.login.required"));
        }
    }

    private void validerEmail(final UserFormData form, final ValidationResult result) {
        if (!isEmailValide(form.getEmail())) {
            result.addError(FIELD_EMAIL, messages.get("validation.email.invalid"));
        }
    }

    private void validerPassword(final UserFormData form, final ValidationResult result) {
        if (!isPasswordValide(form.getPassword())) {
            result.addError(FIELD_PASSWORD,
                    messages.get("validation.password.tooshort", PASSWORD_MIN_LENGTH));
        }
    }

    private void validerPasswordConfirmation(final UserFormData form, final ValidationResult result) {
        if (!isPasswordConfirmValide(form.getPassword(), form.getPasswordConfirm())) {
            result.addError(FIELD_PASSWORD_CONFIRM, messages.get("validation.password.mismatch"));
        }
    }

    private void validerRole(final UserFormData form, final ValidationResult result) {
        if (form.getRole() == null) {
            result.addError(FIELD_ROLE, messages.get("validation.role.required"));
        }
    }
}
