package com.example.user.dto;

import com.example.user.model.User.Civilite;
import com.example.user.model.User.Role;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Objet de transfert (DTO) représentant les données brutes saisies dans le formulaire.
 *
 * <p>Ce DTO découple la couche UI du modèle métier {@link com.example.user.model.User}.
 * Il transporte le mot de passe en clair uniquement le temps de la validation et du hachage ;
 * il ne doit <strong>jamais</strong> être persisté tel quel.</p>
 */
public final class UserFormData {

    // -------------------------------------------------------------------------
    // Attributs
    // -------------------------------------------------------------------------

    private final Civilite  civilite;
    private final String    nom;
    private final String    prenom;
    private final LocalDate dateNaissance;
    private final String    login;
    private final String    email;
    private final String    password;           // mot de passe en clair (temporaire)
    private final String    passwordConfirm;
    private final Role      role;
    private final boolean   actif;
    private final String    telephone;
    private final String    service;
    private final String    adresse;

    // -------------------------------------------------------------------------
    // Constructeur privé
    // -------------------------------------------------------------------------

    private UserFormData(final Builder builder) {
        this.civilite        = builder.civilite;
        this.nom             = builder.nom;
        this.prenom          = builder.prenom;
        this.dateNaissance   = builder.dateNaissance;
        this.login           = builder.login;
        this.email           = builder.email;
        this.password        = builder.password;
        this.passwordConfirm = builder.passwordConfirm;
        this.role            = builder.role;
        this.actif           = builder.actif;
        this.telephone       = builder.telephone;
        this.service         = builder.service;
        this.adresse         = builder.adresse;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Civilite  civilite;
        private String    nom        = "";
        private String    prenom     = "";
        private LocalDate dateNaissance;
        private String    login      = "";
        private String    email      = "";
        private String    password   = "";
        private String    passwordConfirm = "";
        private Role      role;
        private boolean   actif      = true;
        private String    telephone  = "";
        private String    service    = "";
        private String    adresse    = "";

        private Builder() {}

        public Builder civilite(final Civilite v)       { this.civilite = v;         return this; }
        public Builder nom(final String v)              { this.nom = nullToEmpty(v);  return this; }
        public Builder prenom(final String v)           { this.prenom = nullToEmpty(v); return this; }
        public Builder dateNaissance(final LocalDate v) { this.dateNaissance = v;    return this; }
        public Builder login(final String v)            { this.login = nullToEmpty(v); return this; }
        public Builder email(final String v)            { this.email = nullToEmpty(v); return this; }
        public Builder password(final String v)         { this.password = nullToEmpty(v); return this; }
        public Builder passwordConfirm(final String v)  { this.passwordConfirm = nullToEmpty(v); return this; }
        public Builder role(final Role v)               { this.role = v;             return this; }
        public Builder actif(final boolean v)           { this.actif = v;            return this; }
        public Builder telephone(final String v)        { this.telephone = nullToEmpty(v); return this; }
        public Builder service(final String v)          { this.service = nullToEmpty(v); return this; }
        public Builder adresse(final String v)          { this.adresse = nullToEmpty(v); return this; }

        public UserFormData build() {
            return new UserFormData(this);
        }

        private static String nullToEmpty(final String value) {
            return value == null ? "" : value;
        }
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Civilite  getCivilite()        { return civilite; }
    public String    getNom()             { return nom; }
    public String    getPrenom()          { return prenom; }
    public LocalDate getDateNaissance()   { return dateNaissance; }
    public String    getLogin()           { return login; }
    public String    getEmail()           { return email; }
    public String    getPassword()        { return password; }
    public String    getPasswordConfirm() { return passwordConfirm; }
    public Role      getRole()            { return role; }
    public boolean   isActif()            { return actif; }
    public String    getTelephone()       { return telephone; }
    public String    getService()         { return service; }
    public String    getAdresse()         { return adresse; }

    @Override
    public String toString() {
        // Volontairement : ne jamais exposer le mot de passe dans les logs
        return "UserFormData{login='" + login + "', email='" + email + "'}";
    }
}
