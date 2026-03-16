package com.example.user.ui.presenter;

import com.example.user.dto.UserFormData;
import com.example.user.exception.UserAlreadyExistsException;
import com.example.user.exception.UserException;
import com.example.user.i18n.MessageProvider;
import com.example.user.mapper.IUserMapper;
import com.example.user.model.User;
import com.example.user.service.IUserService;
import com.example.user.ui.state.UserFormState;
import com.example.user.validation.IUserFormValidator;
import com.example.user.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Presenter du formulaire utilisateur (pattern MVP).
 *
 * <h3>Responsabilités :</h3>
 * <ol>
 *   <li>Lire l'état depuis {@link UserFormState}.</li>
 *   <li>Orchestrer validation → mapping → persistance.</li>
 *   <li>Écrire le résultat (succès, erreurs) dans {@link UserFormState}.</li>
 * </ol>
 *
 * <p><strong>Aucun import JavaFX</strong> : ce composant est testable avec
 * un simple JUnit sans lancer le toolkit JavaFX.</p>
 */
@Component
public class UserFormPresenter implements IUserFormPresenter {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserFormPresenter.class);

    // -------------------------------------------------------------------------
    // Dépendances
    // -------------------------------------------------------------------------

    private final IUserService       userService;
    private final IUserFormValidator validator;
    private final IUserMapper        mapper;
    private final MessageProvider    messages;
    private final UserFormState      state;

    public UserFormPresenter(final IUserService       userService,
                             final IUserFormValidator validator,
                             final IUserMapper        mapper,
                             final MessageProvider    messages,
                             final UserFormState      state) {
        this.userService = Objects.requireNonNull(userService,  "IUserService est obligatoire.");
        this.validator   = Objects.requireNonNull(validator,    "IUserFormValidator est obligatoire.");
        this.mapper      = Objects.requireNonNull(mapper,       "IUserMapper est obligatoire.");
        this.messages    = Objects.requireNonNull(messages,     "MessageProvider est obligatoire.");
        this.state       = Objects.requireNonNull(state,        "UserFormState est obligatoire.");
    }

    // -------------------------------------------------------------------------
    // Actions (IUserFormPresenter)
    // -------------------------------------------------------------------------

    @Override
    public void enregistrer() {
        state.masquerErreur();
        state.clearFieldErrors();
        state.setEnCours(true);

        try {
            final UserFormData    formData   = construireFormData();
            final ValidationResult validation = validator.validate(formData);

            if (!validation.isValid()) {
                validation.getFieldErrors().forEach(error -> state.setFieldError(error.getField(), error.getMessage()));
                state.afficherErreur(String.join("\n", validation.getMessages()));
                return;
            }

            persister(formData);

        } finally {
            state.setEnCours(false);
        }
    }

    @Override
    public void reinitialiser() {
        LOGGER.debug("Réinitialisation du formulaire.");
        state.reinitialiser();
    }

    @Override
    public boolean validerEmail(final String email) {
        return validator.isEmailValide(email);
    }

    @Override
    public boolean validerPassword(final String password) {
        return validator.isPasswordValide(password);
    }

    @Override
    public boolean validerPasswordConfirm(final String password, final String passwordConfirm) {
        return validator.isPasswordConfirmValide(password, passwordConfirm);
    }

    @Override
    public boolean validerChampObligatoire(final String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    public boolean validerSelectionObligatoire(final Object value) {
        return value != null;
    }

    // -------------------------------------------------------------------------
    // Privé — construction du DTO depuis l'état
    // -------------------------------------------------------------------------

    /**
     * Construit le {@link UserFormData} à partir de l'état courant du formulaire.
     * Toutes les valeurs textuelles sont trimmées ici, une seule fois.
     */
    private UserFormData construireFormData() {
        return UserFormData.builder()
                .civilite(state.getCivilite())
                .nom(trimmed(state.getNom()))
                .prenom(trimmed(state.getPrenom()))
                .dateNaissance(state.getDateNaissance())
                .login(trimmed(state.getLogin()))
                .email(trimmed(state.getEmail()))
                .password(state.getPassword())
                .passwordConfirm(state.getPasswordConfirm())
                .role(state.getRole())
                .actif(state.isActif())
                .telephone(trimmed(state.getTelephone()))
                .service(trimmed(state.getService()))
                .adresse(trimmed(state.getAdresse()))
                .build();
    }

    // -------------------------------------------------------------------------
    // Privé — persistance avec gestion d'erreurs précise
    // -------------------------------------------------------------------------

    private void persister(final UserFormData formData) {
        try {
            final User user = mapper.toUser(formData);
            userService.save(user);
            LOGGER.info("Utilisateur '{}' enregistré avec succès.", user.getLogin());
            state.reinitialiser();      // succès → réinitialisation

        } catch (UserAlreadyExistsException e) {
            LOGGER.warn("Login en doublon : '{}'.", e.getLogin());
            state.afficherErreur(messages.get("ui.error.login.exists", e.getLogin()));

        } catch (UserException e) {
            LOGGER.error("Erreur fonctionnelle lors de l'enregistrement.", e);
            state.afficherErreur(e.getMessage());

        } catch (Exception e) {
            LOGGER.error("Erreur technique lors de l'enregistrement.", e);
            state.afficherErreur(messages.get("ui.error.technical"));
        }
    }

    // -------------------------------------------------------------------------
    // Utilitaire
    // -------------------------------------------------------------------------

    private static String trimmed(final String value) {
        return value == null ? "" : value.trim();
    }
}
