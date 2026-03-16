package com.example.user.ui.state;

import com.example.user.model.User.Civilite;
import com.example.user.model.User.Role;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.beans.property.*;

import java.time.LocalDate;

/**
 * État réactif du formulaire utilisateur.
 *
 * <p>Centralise toutes les {@link javafx.beans.property.Property} JavaFX décrivant
 * l'état courant de l'écran. Le controller lit/écrit ces propriétés ;
 * le presenter les observe pour déclencher les actions métier.</p>
 *
 * <p>Avantages :</p>
 * <ul>
 *   <li>Testable sans contexte JavaFX (propriétés simples Java).</li>
 *   <li>Aucune dépendance vers le presenter ou le controller.</li>
 *   <li>Source de vérité unique pour l'état de l'écran.</li>
 * </ul>
 */
public final class UserFormState {

    // -------------------------------------------------------------------------
    // Identité
    // -------------------------------------------------------------------------

    private final ObjectProperty<Civilite> civilite      = new SimpleObjectProperty<>();
    private final StringProperty           nom           = new SimpleStringProperty("");
    private final StringProperty           prenom        = new SimpleStringProperty("");
    private final ObjectProperty<LocalDate> dateNaissance = new SimpleObjectProperty<>();

    // -------------------------------------------------------------------------
    // Compte
    // -------------------------------------------------------------------------

    private final StringProperty           login           = new SimpleStringProperty("");
    private final StringProperty           email           = new SimpleStringProperty("");
    private final StringProperty           password        = new SimpleStringProperty("");
    private final StringProperty           passwordConfirm = new SimpleStringProperty("");
    private final ObjectProperty<Role>     role            = new SimpleObjectProperty<>();
    private final BooleanProperty          actif           = new SimpleBooleanProperty(true);

    // -------------------------------------------------------------------------
    // Coordonnées
    // -------------------------------------------------------------------------

    private final StringProperty telephone = new SimpleStringProperty("");
    private final StringProperty service   = new SimpleStringProperty("");
    private final StringProperty adresse   = new SimpleStringProperty("");

    // -------------------------------------------------------------------------
    // État UI (feedback)
    // -------------------------------------------------------------------------

    /** Message d'erreur global à afficher dans le bandeau (vide = aucun). */
    private final StringProperty  erreurMessage  = new SimpleStringProperty("");
    private final BooleanProperty erreurVisible  = new SimpleBooleanProperty(false);

    /** Indique si une opération asynchrone est en cours (désactive les boutons). */
    private final BooleanProperty enCours        = new SimpleBooleanProperty(false);
    private final MapProperty<String, String> fieldErrors =
            new SimpleMapProperty<>(FXCollections.observableHashMap());

    // -------------------------------------------------------------------------
    // Accesseurs — Identité
    // -------------------------------------------------------------------------

    public ObjectProperty<Civilite> civiliteProperty()       { return civilite; }
    public StringProperty           nomProperty()            { return nom; }
    public StringProperty           prenomProperty()         { return prenom; }
    public ObjectProperty<LocalDate> dateNaissanceProperty() { return dateNaissance; }

    public Civilite  getCivilite()      { return civilite.get(); }
    public String    getNom()           { return nom.get(); }
    public String    getPrenom()        { return prenom.get(); }
    public LocalDate getDateNaissance() { return dateNaissance.get(); }

    // -------------------------------------------------------------------------
    // Accesseurs — Compte
    // -------------------------------------------------------------------------

    public StringProperty       loginProperty()           { return login; }
    public StringProperty       emailProperty()           { return email; }
    public StringProperty       passwordProperty()        { return password; }
    public StringProperty       passwordConfirmProperty() { return passwordConfirm; }
    public ObjectProperty<Role> roleProperty()            { return role; }
    public BooleanProperty      actifProperty()           { return actif; }

    public String  getLogin()           { return login.get(); }
    public String  getEmail()           { return email.get(); }
    public String  getPassword()        { return password.get(); }
    public String  getPasswordConfirm() { return passwordConfirm.get(); }
    public Role    getRole()            { return role.get(); }
    public boolean isActif()            { return actif.get(); }

    // -------------------------------------------------------------------------
    // Accesseurs — Coordonnées
    // -------------------------------------------------------------------------

    public StringProperty telephoneProperty() { return telephone; }
    public StringProperty serviceProperty()   { return service; }
    public StringProperty adresseProperty()   { return adresse; }

    public String getTelephone() { return telephone.get(); }
    public String getService()   { return service.get(); }
    public String getAdresse()   { return adresse.get(); }

    // -------------------------------------------------------------------------
    // Accesseurs — État UI
    // -------------------------------------------------------------------------

    public StringProperty  erreurMessageProperty() { return erreurMessage; }
    public BooleanProperty erreurVisibleProperty() { return erreurVisible; }
    public BooleanProperty enCoursProperty()       { return enCours; }
    public MapProperty<String, String> fieldErrorsProperty() { return fieldErrors; }

    public String  getErreurMessage() { return erreurMessage.get(); }
    public boolean isErreurVisible()  { return erreurVisible.get(); }
    public boolean isEnCours()        { return enCours.get(); }
    public ObservableMap<String, String> getFieldErrors() { return fieldErrors.get(); }

    // -------------------------------------------------------------------------
    // Mutations d'état (appelées exclusivement par le Presenter)
    // -------------------------------------------------------------------------

    public void afficherErreur(final String message) {
        erreurMessage.set(message);
        erreurVisible.set(true);
    }

    public void masquerErreur() {
        erreurMessage.set("");
        erreurVisible.set(false);
    }

    public void setEnCours(final boolean valeur) {
        enCours.set(valeur);
    }

    public void setFieldError(final String field, final String message) {
        fieldErrors.put(field, message);
    }

    public void clearFieldError(final String field) {
        fieldErrors.remove(field);
    }

    public boolean hasFieldError(final String field) {
        return fieldErrors.containsKey(field);
    }

    public void clearFieldErrors() {
        fieldErrors.clear();
    }

    /**
     * Remet tous les champs à leur valeur initiale.
     */
    public void reinitialiser() {
        civilite.set(null);
        nom.set("");
        prenom.set("");
        dateNaissance.set(null);
        login.set("");
        email.set("");
        password.set("");
        passwordConfirm.set("");
        role.set(null);
        actif.set(true);
        telephone.set("");
        service.set("");
        adresse.set("");
        clearFieldErrors();
        masquerErreur();
    }
}
