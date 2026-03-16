package com.example.user.exception;

/**
 * Levée lorsqu'un utilisateur avec le même identifiant existe déjà en base.
 */
public class UserAlreadyExistsException extends UserException {

    private static final long serialVersionUID = 1L;

    private final String login;

    public UserAlreadyExistsException(final String login) {
        super("L'identifiant '" + login + "' est déjà utilisé.");
        this.login = login;
    }

    /**
     * Retourne l'identifiant en conflit.
     *
     * @return le login déjà présent en base.
     */
    public String getLogin() {
        return login;
    }
}
