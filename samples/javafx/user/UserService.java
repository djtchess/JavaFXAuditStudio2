package com.example.user.service;

import com.example.user.exception.UserException;
import com.example.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Implémentation du service de gestion des utilisateurs.
 *
 * <p>Orchestre la validation des préconditions métier et délègue
 * la persistance au repository (à brancher selon l'architecture : JPA, REST…).</p>
 */
@Service
public class UserService implements IUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    // Injecter ici le repository selon l'architecture :
    // private final UserRepository userRepository;
    //
    // public UserService(final UserRepository userRepository) {
    //     this.userRepository = Objects.requireNonNull(userRepository);
    // }

    @Override
    public void save(final User user) {
        Objects.requireNonNull(user, "L'utilisateur ne peut pas être null.");

        LOGGER.info("Sauvegarde de l'utilisateur : {}", user);

        // TODO : vérifier unicité du login via repository
        // if (userRepository.existsByLogin(user.getLogin())) {
        //     throw new UserAlreadyExistsException(user.getLogin());
        // }

        // TODO : déléguer au repository JPA / appel REST
        // userRepository.save(user);

        LOGGER.info("Utilisateur '{}' enregistré avec succès.", user.getLogin());
    }
}
