package com.example.user.exception;

/**
 * Exception de base pour toutes les erreurs fonctionnelles liées aux utilisateurs.
 *
 * <p>Hériter de {@link RuntimeException} permet de ne pas forcer les catch
 * dans les couches supérieures tout en restant explicite sur les erreurs métier.</p>
 */
public class UserException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UserException(final String message) {
        super(message);
    }

    public UserException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
