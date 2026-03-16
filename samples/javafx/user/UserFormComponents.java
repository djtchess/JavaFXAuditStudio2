package com.example.user.ui.binding;

import com.example.user.model.User.Civilite;
import com.example.user.model.User.Role;
import javafx.scene.control.*;

import java.util.Objects;

/**
 * Value object regroupant les composants JavaFX du formulaire.
 *
 * <p>Permet de passer tous les composants au {@link UserFormBindingManager}
 * en un seul paramètre, sans coupler le manager au controller.
 * Immuable après construction.</p>
 */
public final class UserFormComponents {

    // -------------------------------------------------------------------------
    // Identité
    // -------------------------------------------------------------------------

    final ComboBox<Civilite> cmbCivilite;
    final TextField          txtNom;
    final TextField          txtPrenom;
    final DatePicker         dpDateNaissance;

    // -------------------------------------------------------------------------
    // Compte
    // -------------------------------------------------------------------------

    final TextField      txtLogin;
    final TextField      txtEmail;
    final PasswordField  txtPassword;
    final PasswordField  txtPasswordConfirm;
    final ComboBox<Role> cmbRole;
    final CheckBox       chkActif;

    // -------------------------------------------------------------------------
    // Coordonnées
    // -------------------------------------------------------------------------

    final TextField txtTelephone;
    final TextField txtService;
    final TextArea  txtAdresse;

    // -------------------------------------------------------------------------
    // Feedback & actions
    // -------------------------------------------------------------------------

    final Label  lblErreur;
    final Button btnEnregistrer;
    final Button btnReinitialiser;
    final Button btnAnnuler;

    // -------------------------------------------------------------------------
    // Constructeur (Builder pour lisibilité)
    // -------------------------------------------------------------------------

    private UserFormComponents(final Builder builder) {
        this.cmbCivilite       = requireNonNull(builder.cmbCivilite,       "cmbCivilite");
        this.txtNom            = requireNonNull(builder.txtNom,            "txtNom");
        this.txtPrenom         = requireNonNull(builder.txtPrenom,         "txtPrenom");
        this.dpDateNaissance   = requireNonNull(builder.dpDateNaissance,   "dpDateNaissance");
        this.txtLogin          = requireNonNull(builder.txtLogin,          "txtLogin");
        this.txtEmail          = requireNonNull(builder.txtEmail,          "txtEmail");
        this.txtPassword       = requireNonNull(builder.txtPassword,       "txtPassword");
        this.txtPasswordConfirm= requireNonNull(builder.txtPasswordConfirm,"txtPasswordConfirm");
        this.cmbRole           = requireNonNull(builder.cmbRole,           "cmbRole");
        this.chkActif          = requireNonNull(builder.chkActif,          "chkActif");
        this.txtTelephone      = requireNonNull(builder.txtTelephone,      "txtTelephone");
        this.txtService        = requireNonNull(builder.txtService,        "txtService");
        this.txtAdresse        = requireNonNull(builder.txtAdresse,        "txtAdresse");
        this.lblErreur         = requireNonNull(builder.lblErreur,         "lblErreur");
        this.btnEnregistrer    = requireNonNull(builder.btnEnregistrer,    "btnEnregistrer");
        this.btnReinitialiser  = requireNonNull(builder.btnReinitialiser,  "btnReinitialiser");
        this.btnAnnuler        = requireNonNull(builder.btnAnnuler,        "btnAnnuler");
    }

    private static <T> T requireNonNull(final T value, final String name) {
        return Objects.requireNonNull(value, "Composant FXML manquant : " + name);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private ComboBox<Civilite> cmbCivilite;
        private TextField          txtNom;
        private TextField          txtPrenom;
        private DatePicker         dpDateNaissance;
        private TextField          txtLogin;
        private TextField          txtEmail;
        private PasswordField      txtPassword;
        private PasswordField      txtPasswordConfirm;
        private ComboBox<Role>     cmbRole;
        private CheckBox           chkActif;
        private TextField          txtTelephone;
        private TextField          txtService;
        private TextArea           txtAdresse;
        private Label              lblErreur;
        private Button             btnEnregistrer;
        private Button             btnReinitialiser;
        private Button             btnAnnuler;

        private Builder() {}

        public Builder cmbCivilite(ComboBox<Civilite> v)    { this.cmbCivilite = v;        return this; }
        public Builder txtNom(TextField v)                  { this.txtNom = v;             return this; }
        public Builder txtPrenom(TextField v)               { this.txtPrenom = v;          return this; }
        public Builder dpDateNaissance(DatePicker v)        { this.dpDateNaissance = v;    return this; }
        public Builder txtLogin(TextField v)                { this.txtLogin = v;           return this; }
        public Builder txtEmail(TextField v)                { this.txtEmail = v;           return this; }
        public Builder txtPassword(PasswordField v)         { this.txtPassword = v;        return this; }
        public Builder txtPasswordConfirm(PasswordField v)  { this.txtPasswordConfirm = v; return this; }
        public Builder cmbRole(ComboBox<Role> v)            { this.cmbRole = v;            return this; }
        public Builder chkActif(CheckBox v)                 { this.chkActif = v;           return this; }
        public Builder txtTelephone(TextField v)            { this.txtTelephone = v;       return this; }
        public Builder txtService(TextField v)              { this.txtService = v;         return this; }
        public Builder txtAdresse(TextArea v)               { this.txtAdresse = v;         return this; }
        public Builder lblErreur(Label v)                   { this.lblErreur = v;          return this; }
        public Builder btnEnregistrer(Button v)             { this.btnEnregistrer = v;     return this; }
        public Builder btnReinitialiser(Button v)           { this.btnReinitialiser = v;   return this; }
        public Builder btnAnnuler(Button v)                 { this.btnAnnuler = v;         return this; }

        public UserFormComponents build() {
            return new UserFormComponents(this);
        }
    }
}
