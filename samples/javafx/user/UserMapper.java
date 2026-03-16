package com.example.user.mapper;

import com.example.user.dto.UserFormData;
import com.example.user.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Implémentation de la conversion {@link UserFormData} → {@link User}.
 *
 * <p>Le mot de passe est haché via {@link PasswordEncoder} (BCrypt par défaut)
 * afin de ne jamais stocker ni transmettre de mot de passe en clair.</p>
 */
@Component
public class UserMapper implements IUserMapper {

    private final PasswordEncoder passwordEncoder;

    public UserMapper(final PasswordEncoder passwordEncoder) {
        this.passwordEncoder = Objects.requireNonNull(
                passwordEncoder, "PasswordEncoder est obligatoire.");
    }

    @Override
    public User toUser(final UserFormData form) {
        Objects.requireNonNull(form, "Le formulaire ne peut pas être null.");

        return User.builder()
                .civilite(form.getCivilite())
                .nom(form.getNom())
                .prenom(form.getPrenom())
                .dateNaissance(form.getDateNaissance())
                .login(form.getLogin())
                .email(form.getEmail())
                .passwordHash(passwordEncoder.encode(form.getPassword()))
                .role(form.getRole())
                .actif(form.isActif())
                .telephone(form.getTelephone())
                .service(form.getService())
                .adresse(form.getAdresse())
                .build();
    }
}
