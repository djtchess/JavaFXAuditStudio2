package com.example.user.controller;

import com.example.user.model.User.Civilite;
import com.example.user.model.User.Role;
import com.example.user.ui.binding.UserFormBindingManager;
import com.example.user.ui.binding.UserFormComponents;
import com.example.user.ui.presenter.IUserFormPresenter;
import com.example.user.ui.state.UserFormState;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Contrôleur JavaFX du formulaire utilisateur.
 *
 * <h3>Responsabilité unique : coller la vue aux autres composants.</h3>
 * <ol>
 *   <li>Alimenter les ComboBox avec leurs valeurs d'énumération.</li>
 *   <li>Déléguer tous les bindings/listeners au {@link UserFormBindingManager}.</li>
 *   <li>Transmettre les actions boutons au {@link IUserFormPresenter}.</li>
 * </ol>
 *
 * <p>Ce controller ne contient aucune règle de validation, aucune logique
 * métier, aucun listener instancié directement. Il ne dépasse pas ~80 lignes.</p>
 */
@Component
public class UserFormController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserFormController.class);

    // -------------------------------------------------------------------------
    // Dépendances (injection par constructeur)
    // -------------------------------------------------------------------------

    private final IUserFormPresenter    presenter;
    private final UserFormState         state;
    private final UserFormBindingManager bindingManager;

    public UserFormController(final IUserFormPresenter     presenter,
                              final UserFormState          state,
                              final UserFormBindingManager bindingManager) {
        this.presenter      = Objects.requireNonNull(presenter,      "IUserFormPresenter est obligatoire.");
        this.state          = Objects.requireNonNull(state,          "UserFormState est obligatoire.");
        this.bindingManager = Objects.requireNonNull(bindingManager, "UserFormBindingManager est obligatoire.");
    }

    // -------------------------------------------------------------------------
    // Composants FXML (déclarés ici pour injection par le FXMLLoader)
    // -------------------------------------------------------------------------

    @FXML private ComboBox<Civilite> cmbCivilite;
    @FXML private TextField          txtNom;
    @FXML private TextField          txtPrenom;
    @FXML private DatePicker         dpDateNaissance;

    @FXML private TextField      txtLogin;
    @FXML private TextField      txtEmail;
    @FXML private PasswordField  txtPassword;
    @FXML private PasswordField  txtPasswordConfirm;
    @FXML private ComboBox<Role> cmbRole;
    @FXML private CheckBox       chkActif;

    @FXML private TextField txtTelephone;
    @FXML private TextField txtService;
    @FXML private TextArea  txtAdresse;

    @FXML private Label  lblErreur;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnReinitialiser;
    @FXML private Button btnAnnuler;

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        LOGGER.debug("Initialisation du contrôleur UserForm.");
        initComboBoxes();
        bindingManager.bind(buildComponents());
    }

    private void initComboBoxes() {
        cmbCivilite.setItems(FXCollections.observableArrayList(Civilite.values()));
        cmbRole.setItems(FXCollections.observableArrayList(Role.values()));
    }

    /**
     * Assemble le value object {@link UserFormComponents} à partir des champs {@code @FXML}.
     */
    private UserFormComponents buildComponents() {
        return UserFormComponents.builder()
                .cmbCivilite(cmbCivilite)
                .txtNom(txtNom)
                .txtPrenom(txtPrenom)
                .dpDateNaissance(dpDateNaissance)
                .txtLogin(txtLogin)
                .txtEmail(txtEmail)
                .txtPassword(txtPassword)
                .txtPasswordConfirm(txtPasswordConfirm)
                .cmbRole(cmbRole)
                .chkActif(chkActif)
                .txtTelephone(txtTelephone)
                .txtService(txtService)
                .txtAdresse(txtAdresse)
                .lblErreur(lblErreur)
                .btnEnregistrer(btnEnregistrer)
                .btnReinitialiser(btnReinitialiser)
                .btnAnnuler(btnAnnuler)
                .build();
    }

    // -------------------------------------------------------------------------
    // Handlers FXML (délégation pure, aucune logique)
    // -------------------------------------------------------------------------

    @FXML
    private void handleEnregistrer() {
        presenter.enregistrer();
    }

    @FXML
    private void handleReinitialiser() {
        presenter.reinitialiser();
    }

    @FXML
    private void handleAnnuler() {
        if (btnAnnuler.getScene() != null && btnAnnuler.getScene().getWindow() != null) {
            btnAnnuler.getScene().getWindow().hide();
            return;
        }
        LOGGER.warn("Fermeture du formulaire ignoree: scene ou window absente.");
    }
}
