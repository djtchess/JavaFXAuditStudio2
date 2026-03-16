package com.example.user.service;

import com.example.user.exception.UserAlreadyExistsException;
import com.example.user.exception.UserException;
import com.example.user.model.User;

/**
 * Contrat métier pour la gestion des utilisateurs.
 */
public interface IUserService {

    /**
     * Enregistre un nouvel utilisateur.
     *
     * @param user l'utilisateur à persister — ne doit pas être {@code null}.
     * @throws UserAlreadyExistsException si un utilisateur avec le même login existe déjà.
     * @throws UserException              pour toute autre erreur fonctionnelle.
     */
    void save(User user);
}
