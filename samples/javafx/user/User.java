package com.example.user.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entité métier représentant un utilisateur de l'application.
 *
 * <p>Immuable après construction via {@link Builder}.</p>
 */
public final class User {

    // -------------------------------------------------------------------------
    // Énumérations
    // -------------------------------------------------------------------------

    public enum Civilite {
        M("M."), MME("Mme"), DR("Dr"), PR("Pr");

        private final String libelle;

        Civilite(final String libelle) {
            this.libelle = libelle;
        }

        @Override
        public String toString() {
            return libelle;
        }
    }

    public enum Role {
        ADMINISTRATEUR, GESTIONNAIRE, CONSULTANT, LECTEUR
    }

    // -------------------------------------------------------------------------
    // Attributs (tous finals — immuabilité)
    // -------------------------------------------------------------------------

    private final Long      id;
    private final Civilite  civilite;
    private final String    nom;
    private final String    prenom;
    private final LocalDate dateNaissance;
    private final String    login;
    private final String    email;
    private final String    passwordHash;   // jamais le mot de passe en clair
    private final Role      role;
    private final boolean   actif;
    private final String    telephone;
    private final String    service;
    private final String    adresse;

    // -------------------------------------------------------------------------
    // Constructeur privé (usage exclusif du Builder)
    // -------------------------------------------------------------------------

    private User(final Builder builder) {
        this.id            = builder.id;
        this.civilite      = Objects.requireNonNull(builder.civilite,   "La civilité est obligatoire.");
        this.nom           = Objects.requireNonNull(builder.nom,        "Le nom est obligatoire.");
        this.prenom        = Objects.requireNonNull(builder.prenom,     "Le prénom est obligatoire.");
        this.login         = Objects.requireNonNull(builder.login,      "L'identifiant est obligatoire.");
        this.email         = Objects.requireNonNull(builder.email,      "L'email est obligatoire.");
        this.passwordHash  = Objects.requireNonNull(builder.passwordHash, "Le mot de passe est obligatoire.");
        this.role          = Objects.requireNonNull(builder.role,       "Le rôle est obligatoire.");
        this.dateNaissance = builder.dateNaissance;
        this.actif         = builder.actif;
        this.telephone     = builder.telephone;
        this.service       = builder.service;
        this.adresse       = builder.adresse;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Long      id;
        private Civilite  civilite;
        private String    nom;
        private String    prenom;
        private LocalDate dateNaissance;
        private String    login;
        private String    email;
        private String    passwordHash;
        private Role      role;
        private boolean   actif  = true;
        private String    telephone;
        private String    service;
        private String    adresse;

        private Builder() {}

        public Builder id(final Long v)               { this.id = v;            return this; }
        public Builder civilite(final Civilite v)     { this.civilite = v;      return this; }
        public Builder nom(final String v)            { this.nom = v;           return this; }
        public Builder prenom(final String v)         { this.prenom = v;        return this; }
        public Builder dateNaissance(final LocalDate v){ this.dateNaissance = v; return this; }
        public Builder login(final String v)          { this.login = v;         return this; }
        public Builder email(final String v)          { this.email = v;         return this; }
        public Builder passwordHash(final String v)   { this.passwordHash = v;  return this; }
        public Builder role(final Role v)             { this.role = v;          return this; }
        public Builder actif(final boolean v)         { this.actif = v;         return this; }
        public Builder telephone(final String v)      { this.telephone = v;     return this; }
        public Builder service(final String v)        { this.service = v;       return this; }
        public Builder adresse(final String v)        { this.adresse = v;       return this; }

        public User build() {
            return new User(this);
        }
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long      getId()            { return id; }
    public Civilite  getCivilite()      { return civilite; }
    public String    getNom()           { return nom; }
    public String    getPrenom()        { return prenom; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public String    getLogin()         { return login; }
    public String    getEmail()         { return email; }
    public String    getPasswordHash()  { return passwordHash; }
    public Role      getRole()          { return role; }
    public boolean   isActif()          { return actif; }
    public String    getTelephone()     { return telephone; }
    public String    getService()       { return service; }
    public String    getAdresse()       { return adresse; }

    // -------------------------------------------------------------------------
    // equals / hashCode / toString
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        final User user = (User) o;
        return Objects.equals(login, user.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login);
    }

    @Override
    public String toString() {
        return "User{login='" + login + "', email='" + email + "', role=" + role + '}';
    }
}
