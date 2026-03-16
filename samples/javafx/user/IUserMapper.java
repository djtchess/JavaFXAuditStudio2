package com.example.user.mapper;

import com.example.user.dto.UserFormData;
import com.example.user.model.User;

/**
 * Contrat de conversion entre le DTO de saisie et l'entité métier.
 */
public interface IUserMapper {

    /**
     * Convertit un {@link UserFormData} validé en entité {@link User}.
     *
     * <p>Le mot de passe est haché lors de cette conversion ;
     * le DTO ne devrait plus être utilisé après cet appel.</p>
     *
     * @param form le DTO de saisie — ne doit pas être {@code null}.
     * @return une nouvelle instance {@link User} prête à être persistée.
     */
    User toUser(UserFormData form);
}
