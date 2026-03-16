package com.example.user.ui.binding;

import com.example.user.ui.presenter.IUserFormPresenter;
import com.example.user.ui.state.UserFormState;
import javafx.scene.control.*;

import java.util.Objects;
import java.util.function.Supplier;

import static com.example.user.validation.ValidationResult.FIELD_CIVILITE;
import static com.example.user.validation.ValidationResult.FIELD_EMAIL;
import static com.example.user.validation.ValidationResult.FIELD_LOGIN;
import static com.example.user.validation.ValidationResult.FIELD_NOM;
import static com.example.user.validation.ValidationResult.FIELD_PASSWORD;
import static com.example.user.validation.ValidationResult.FIELD_PASSWORD_CONFIRM;
import static com.example.user.validation.ValidationResult.FIELD_PRENOM;
import static com.example.user.validation.ValidationResult.FIELD_ROLE;

/**
 * Gestionnaire centralisé des bindings et listeners JavaFX du formulaire.
 *
 * <h3>Responsabilités :</h3>
 * <ul>
 *   <li>Binding bidirectionnel composants ↔ {@link UserFormState}.</li>
 *   <li>Listeners de validation temps-réel (focus perdu).</li>
 *   <li>Bindings de l'état UI (visibilité erreurs, désactivation boutons).</li>
 * </ul>
 *
 * <p>Le controller délègue entièrement la configuration des bindings à cette
 * classe, ce qui le décharge de toute logique d'état et de réactivité.</p>
 */
public final class UserFormBindingManager {

    private final UserFormState      state;
    private final IUserFormPresenter presenter;

    public UserFormBindingManager(final UserFormState      state,
                                  final IUserFormPresenter presenter) {
        this.state     = Objects.requireNonNull(state,     "UserFormState est obligatoire.");
        this.presenter = Objects.requireNonNull(presenter, "IUserFormPresenter est obligatoire.");
    }

    // =========================================================================
    // Point d'entrée unique appelé par le controller
    // =========================================================================

    /**
     * Installe tous les bindings et listeners sur les composants fournis.
     * À appeler une seule fois dans {@code initialize()}.
     *
     * @param components l'ensemble des composants du formulaire.
     */
    public void bind(final UserFormComponents components) {
        Objects.requireNonNull(components, "UserFormComponents est obligatoire.");
        bindDataFields(components);
        bindValidationListeners(components);
        bindUiState(components);
    }

    // =========================================================================
    // Bindings données (composant ↔ state)
    // =========================================================================

    /**
     * Lie chaque composant de saisie à la propriété correspondante dans l'état.
     */
    private void bindDataFields(final UserFormComponents c) {
        // Identité
        c.cmbCivilite.valueProperty().bindBidirectional(state.civiliteProperty());
        c.txtNom.textProperty().bindBidirectional(state.nomProperty());
        c.txtPrenom.textProperty().bindBidirectional(state.prenomProperty());
        c.dpDateNaissance.valueProperty().bindBidirectional(state.dateNaissanceProperty());

        // Compte
        c.txtLogin.textProperty().bindBidirectional(state.loginProperty());
        c.txtEmail.textProperty().bindBidirectional(state.emailProperty());
        c.txtPassword.textProperty().bindBidirectional(state.passwordProperty());
        c.txtPasswordConfirm.textProperty().bindBidirectional(state.passwordConfirmProperty());
        c.cmbRole.valueProperty().bindBidirectional(state.roleProperty());
        c.chkActif.selectedProperty().bindBidirectional(state.actifProperty());

        // Coordonnées
        c.txtTelephone.textProperty().bindBidirectional(state.telephoneProperty());
        c.txtService.textProperty().bindBidirectional(state.serviceProperty());
        c.txtAdresse.textProperty().bindBidirectional(state.adresseProperty());
    }

    // =========================================================================
    // Listeners de validation temps-réel
    // =========================================================================

    /**
     * Installe les listeners de validation à la perte de focus.
     * Chaque listener délègue la règle au presenter (qui délègue au validator).
     */
    private void bindValidationListeners(final UserFormComponents c) {
        attachValidationOnFocusLost(c.txtNom,
                () -> presenter.validerChampObligatoire(c.txtNom.getText()),
                () -> state.hasFieldError(FIELD_NOM),
                FIELD_NOM);

        attachValidationOnFocusLost(c.txtPrenom,
                () -> presenter.validerChampObligatoire(c.txtPrenom.getText()),
                () -> state.hasFieldError(FIELD_PRENOM),
                FIELD_PRENOM);

        attachValidationOnFocusLost(c.txtLogin,
                () -> presenter.validerChampObligatoire(c.txtLogin.getText()),
                () -> state.hasFieldError(FIELD_LOGIN),
                FIELD_LOGIN);

        attachValidationOnFocusLost(c.txtEmail,
                () -> presenter.validerEmail(c.txtEmail.getText()),
                () -> state.hasFieldError(FIELD_EMAIL),
                FIELD_EMAIL);

        attachValidationOnFocusLost(c.txtPassword,
                () -> presenter.validerPassword(c.txtPassword.getText()),
                () -> state.hasFieldError(FIELD_PASSWORD),
                FIELD_PASSWORD);

        attachValidationOnFocusLost(c.txtPasswordConfirm,
                () -> presenter.validerPasswordConfirm(
                        c.txtPassword.getText(), c.txtPasswordConfirm.getText()),
                () -> state.hasFieldError(FIELD_PASSWORD_CONFIRM),
                FIELD_PASSWORD_CONFIRM);

        attachSelectionValidation(c.cmbCivilite,
                () -> presenter.validerSelectionObligatoire(c.cmbCivilite.getValue()),
                () -> state.hasFieldError(FIELD_CIVILITE),
                FIELD_CIVILITE);

        attachSelectionValidation(c.cmbRole,
                () -> presenter.validerSelectionObligatoire(c.cmbRole.getValue()),
                () -> state.hasFieldError(FIELD_ROLE),
                FIELD_ROLE);
    }

    /**
     * Attache un listener de style CSS sur focus perdu.
     *
     * @param control  le composant à surveiller.
     * @param isValide fournisseur évaluant la validité au moment du focus perdu.
     */
    private void attachValidationOnFocusLost(final Control control,
                                             final Supplier<Boolean> isValide,
                                             final Supplier<Boolean> hasPersistentError,
                                             final String fieldName) {
        control.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                boolean valid = isValide.get();
                if (valid) {
                    state.clearFieldError(fieldName);
                }
                applyValidationStyle(control, valid && !hasPersistentError.get());
            }
        });
    }

    private void attachSelectionValidation(final ComboBox<?> comboBox,
                                           final Supplier<Boolean> isValide,
                                           final Supplier<Boolean> hasPersistentError,
                                           final String fieldName) {
        comboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            boolean valid = isValide.get();
            if (valid) {
                state.clearFieldError(fieldName);
            }
            applyValidationStyle(comboBox, valid && !hasPersistentError.get());
        });
    }

    private static void applyValidationStyle(final Control control, final boolean valide) {
        if (valide) {
            control.getStyleClass().remove("field-error");
        } else if (!control.getStyleClass().contains("field-error")) {
            control.getStyleClass().add("field-error");
        }
    }

    // =========================================================================
    // Bindings état UI (visibilité, activation)
    // =========================================================================

    /**
     * Lie l'état UI (bandeau d'erreur, désactivation des boutons) aux propriétés de l'état.
     */
    private void bindUiState(final UserFormComponents c) {
        // Bandeau d'erreur
        c.lblErreur.textProperty().bind(state.erreurMessageProperty());
        c.lblErreur.visibleProperty().bind(state.erreurVisibleProperty());
        c.lblErreur.managedProperty().bind(state.erreurVisibleProperty());

        // Désactivation des boutons pendant une opération en cours
        c.btnEnregistrer.disableProperty().bind(state.enCoursProperty());
        c.btnReinitialiser.disableProperty().bind(state.enCoursProperty());
        c.btnAnnuler.disableProperty().bind(state.enCoursProperty());

        state.fieldErrorsProperty().addListener((obs, oldValue, newValue) -> refreshValidationStyles(c));
        refreshValidationStyles(c);
    }

    private void refreshValidationStyles(final UserFormComponents c) {
        applyValidationStyle(c.cmbCivilite, !state.hasFieldError(FIELD_CIVILITE));
        applyValidationStyle(c.txtNom, !state.hasFieldError(FIELD_NOM));
        applyValidationStyle(c.txtPrenom, !state.hasFieldError(FIELD_PRENOM));
        applyValidationStyle(c.txtLogin, !state.hasFieldError(FIELD_LOGIN));
        applyValidationStyle(c.txtEmail, !state.hasFieldError(FIELD_EMAIL));
        applyValidationStyle(c.txtPassword, !state.hasFieldError(FIELD_PASSWORD));
        applyValidationStyle(c.txtPasswordConfirm, !state.hasFieldError(FIELD_PASSWORD_CONFIRM));
        applyValidationStyle(c.cmbRole, !state.hasFieldError(FIELD_ROLE));
    }
}
